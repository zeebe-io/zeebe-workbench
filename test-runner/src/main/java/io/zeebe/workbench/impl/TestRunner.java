package io.zeebe.workbench.impl;

import io.zeebe.broker.Broker;
import io.zeebe.broker.system.configuration.BrokerCfg;
import io.zeebe.broker.system.configuration.TopicCfg;
import io.zeebe.gateway.ZeebeClient;
import io.zeebe.gateway.api.clients.TopicClient;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.model.bpmn.instance.Process;
import io.zeebe.model.bpmn.instance.ServiceTask;
import io.zeebe.model.bpmn.instance.zeebe.ZeebeTaskDefinition;
import io.zeebe.protocol.Protocol;
import io.zeebe.util.sched.clock.ActorClock;
import io.zeebe.workbench.*;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class TestRunner implements Runner, AutoCloseable {

  private final String tempFolder;
  private final Broker broker;

  private final ZeebeClient zeebeClient = ZeebeClient.newClient();
  private final TopicClient topicClient;

  private final HashMap<String, WorkflowResource> deployedResources = new HashMap<>();

  public TestRunner() {
    try {
      tempFolder = Files.createTempDirectory("zeebe").toAbsolutePath().normalize().toString();
      BrokerCfg cfg = new BrokerCfg();
      final TopicCfg defaultTopic = new TopicCfg();
      defaultTopic.setName(Protocol.DEFAULT_TOPIC);
      defaultTopic.setPartitions(1);
      defaultTopic.setReplicationFactor(1);
      cfg.getTopics().add(defaultTopic);
      cfg.setBootstrap(1);
      broker = new Broker(cfg, tempFolder, (ActorClock) null);
    } catch (Exception ex) {
      throw new IllegalStateException("Broker start does not work.");
    }
    topicClient = zeebeClient.topicClient();
  }

  @Override
  public List<TestResult> run(WorkflowResource resources, TestCase cases) {
    return run(Collections.singletonList(resources), Collections.singletonList(cases));
  }

  @Override
  public List<TestResult> run(List<WorkflowResource> resources, List<TestCase> cases) {
    deploy(resources);

    return runTests(cases);
  }

  private void deploy(List<WorkflowResource> resources) {
    for (WorkflowResource resource : resources) {
      deployedResources.put(resource.getName(), resource);

      topicClient
          .workflowClient()
          .newDeployCommand()
          .addResourceBytes(resource.getResource(), resource.getName())
          .send()
          .join();
    }
  }

  private List<TestResult> runTests(List<TestCase> cases) {
    final List<TestResult> results = new ArrayList<>();
    if (cases != null && !cases.isEmpty()) {
      for (TestCase testCase : cases) {
        runTest(testCase);
      }
    }
    return results;
  }

  private TestResult runTest(TestCase testCase) {
    final TestResult result = new TestResult(testCase.getName());

    final String resourceName = testCase.getResourceName();
    final WorkflowResource resource = deployedResources.get(resourceName);
    final InputStream inputStream = new ByteArrayInputStream(resource.getResource());
    final BpmnModelInstance bpmnModelInstance = Bpmn.readModelFromStream(inputStream);
    final Process process =
        bpmnModelInstance
            .getDefinitions()
            .getChildElementsByType(Process.class)
            .stream()
            .findFirst()
            .get();
    final String bpmnProcessId = process.getId();
    final String startPayload = testCase.getStartPayload();

    // TODO start workflow instance with given payLoad
    topicClient
        .workflowClient()
        .newCreateInstanceCommand()
        .bpmnProcessId(bpmnProcessId)
        .latestVersion()
        .payload(startPayload)
        .send()
        .join();

    final List<Command> commands = testCase.getCommands();
    if (commands != null && !commands.isEmpty()) {

      for (Command cmd : commands) {
        try {
          executeCommand(bpmnModelInstance, cmd);
        } catch (InterruptedException ite) {
          throw new RuntimeException(ite);
        }
      }
    }

    final List<Verification> verifications = testCase.getVerifications();
    if (verifications != null && !verifications.isEmpty()) {
      // TODO verify current state
      for (Verification verification : verifications) {
        // TODO verify expected state
        // TODO add failed verification on failed verification
      }
    }

    return result;
  }

  private void executeCommand(BpmnModelInstance bpmnModelInstance, Command cmd)
      throws InterruptedException {
    final ModelElementInstance modelElementById =
        bpmnModelInstance.getModelElementById(cmd.getActivityId());

    if (modelElementById instanceof ServiceTask) {
      final CountDownLatch latch = new CountDownLatch(1);
      final ServiceTask serviceTask = (ServiceTask) modelElementById;

      final ZeebeTaskDefinition zeebeTaskDefinition =
          serviceTask
              .getExtensionElements()
              .getElementsQuery()
              .filterByType(ZeebeTaskDefinition.class)
              .singleResult();
      final String taskType = zeebeTaskDefinition.getType();

      topicClient
          .jobClient()
          .newWorker()
          .jobType(taskType)
          .handler(
              (jobClient, jobEvent) -> {
                latch.countDown();
                jobClient.newCompleteCommand(jobEvent).payload(cmd.getPayload()).send().join();
              })
          .open();

      latch.await();

    } else {
      throw new IllegalArgumentException("Only service tasks are currently supported.");
    }
  }

  @Override
  public void close() throws Exception {
    zeebeClient.close();
    broker.close();
  }
}
