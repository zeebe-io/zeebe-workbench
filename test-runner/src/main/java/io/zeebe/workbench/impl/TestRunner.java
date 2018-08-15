package io.zeebe.workbench.impl;

import io.zeebe.broker.Broker;
import io.zeebe.broker.system.configuration.BrokerCfg;
import io.zeebe.broker.system.configuration.TopicCfg;
import io.zeebe.gateway.ZeebeClient;
import io.zeebe.gateway.api.clients.TopicClient;
import io.zeebe.protocol.Protocol;
import io.zeebe.util.sched.clock.ActorClock;
import io.zeebe.workbench.*;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
    final String startPayload = testCase.getStartPayload();

    // TODO start workflow instance with given payLoad
    topicClient
        .workflowClient()
        .newCreateInstanceCommand()
        .bpmnProcessId(resourceName)
        .latestVersion()
        .payload(testCase.getStartPayload())
        .send()
        .join();

    final List<Command> commands = testCase.getCommands();
    if (commands != null && !commands.isEmpty()) {

      // TODO run complete job commands

    }

    final List<Verification> verifications = testCase.getVerifications();
    if (commands != null && !commands.isEmpty()) {
      // TODO verify current state
      for (Verification verification : verifications) {
        // TODO verify expected state
        // TODO add failed verification on failed verification
      }
    }

    return result;
  }

  @Override
  public void close() throws Exception {
    zeebeClient.close();
    broker.close();
  }
}
