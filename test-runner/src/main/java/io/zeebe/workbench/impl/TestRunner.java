package io.zeebe.workbench.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zeebe.broker.Broker;
import io.zeebe.broker.system.configuration.BrokerCfg;
import io.zeebe.broker.system.configuration.TopicCfg;
import io.zeebe.gateway.ZeebeClient;
import io.zeebe.gateway.api.clients.TopicClient;
import io.zeebe.gateway.api.events.WorkflowInstanceEvent;
import io.zeebe.gateway.api.events.WorkflowInstanceState;
import io.zeebe.gateway.api.subscription.JobWorker;
import io.zeebe.gateway.api.subscription.TopicSubscription;
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
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TestRunner implements Runner, AutoCloseable {

  public static final int TEST_TIMEOUT = 250;
  public static final TimeUnit TEST_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  private final String tempFolder;
  private final Broker broker;

  private final ObjectMapper objectMapper = new ObjectMapper();
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
        final TestResult testResult = runTest(testCase);
        results.add(testResult);
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

    final WorkflowInstanceEvent workflowInstanceEvent =
        topicClient
            .workflowClient()
            .newCreateInstanceCommand()
            .bpmnProcessId(bpmnProcessId)
            .latestVersion()
            .payload(startPayload)
            .send()
            .join();

    final List<Command> commands = testCase.getCommands();
    executeCommands(bpmnModelInstance, commands);

    final List<Verification> verifications = testCase.getVerifications();
    final List<FailedVerification> failedVerifications =
        evaluateVerifications(testCase, workflowInstanceEvent, verifications);
    result.addFailedVerifications(failedVerifications);

    return result;
  }

  private List<FailedVerification> evaluateVerifications(
      TestCase testCase, WorkflowInstanceEvent instanceEvent, List<Verification> verifications) {
    List<FailedVerification> failedVerifications = Collections.EMPTY_LIST;
    if (verifications != null && !verifications.isEmpty()) {
      try {
        failedVerifications = verify(testCase, instanceEvent, verifications);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return failedVerifications;
  }

  private List<FailedVerification> verify(
      TestCase testCase, WorkflowInstanceEvent instanceEvent, List<Verification> verifications)
      throws InterruptedException {
    final Map<String, Verification> verificationMap =
        verifications.stream().collect(Collectors.toMap(v -> v.getActivityId(), v -> v));

    final CountDownLatch latch = new CountDownLatch(1);
    final List<WorkflowInstanceEvent> events = new CopyOnWriteArrayList<>();
    final List<FailedVerification> failedVerifications = new CopyOnWriteArrayList<>();

    TopicSubscription subscription = null;
    try {
      subscription =
          topicClient
              .newSubscription()
              .name(testCase.getName())
              .workflowInstanceEventHandler(
                  workflowInstanceEvent -> {
                    if (workflowInstanceEvent.getWorkflowInstanceKey()
                        != instanceEvent.getWorkflowInstanceKey()) {
                      return;
                    }

                    final Verification verification =
                        verificationMap.get(workflowInstanceEvent.getActivityId());

                    if (verification != null) {
                      final WorkflowInstanceState state = workflowInstanceEvent.getState();
                      if (verification.getExpectedIntent().equalsIgnoreCase(state.name())) {
                        final JsonNode expectedPayload =
                            objectMapper.readTree(verification.getExpectedPayload());
                        final JsonNode actualPayload =
                            objectMapper.readTree(workflowInstanceEvent.getPayload());

                        if (!expectedPayload.equals(actualPayload)) {
                          final FailedVerification failedVerification =
                              new FailedVerification(
                                  verification, workflowInstanceEvent.getPayload());
                          failedVerifications.add(failedVerification);
                        }

                        verificationMap.remove(workflowInstanceEvent.getActivityId());
                      }
                    }

                    events.add(workflowInstanceEvent);

                    if (workflowInstanceEvent.getState() == WorkflowInstanceState.ELEMENT_COMPLETED
                        && workflowInstanceEvent
                            .getActivityId()
                            .equals(instanceEvent.getBpmnProcessId())) {
                      latch.countDown();
                    }
                  })
              .startAtPosition(1, instanceEvent.getMetadata().getPosition())
              .open();

      latch.await(TEST_TIMEOUT, TEST_TIMEOUT_UNIT);
    } finally {
      if (subscription != null) {
        subscription.close();
      }
    }

    final Collection<Verification> remainingVerifications = verificationMap.values();
    for (Verification verification : remainingVerifications) {
      failedVerifications.add(new FailedVerification(verification, null));
    }
    return failedVerifications;
  }

  private void executeCommands(BpmnModelInstance bpmnModelInstance, List<Command> commands) {
    if (commands != null && !commands.isEmpty()) {
      for (Command cmd : commands) {
        try {
          executeCommand(bpmnModelInstance, cmd);
        } catch (InterruptedException ite) {
          throw new RuntimeException(ite);
        }
      }
    }
  }

  private void executeCommand(BpmnModelInstance bpmnModelInstance, Command cmd)
      throws InterruptedException {
    final ModelElementInstance modelElementById =
        bpmnModelInstance.getModelElementById(cmd.getActivityId());

    if (modelElementById instanceof ServiceTask) {
      executeServiceTaskCommands(cmd, (ServiceTask) modelElementById);
    } else {
      throw new IllegalArgumentException("Only service tasks are currently supported.");
    }
  }

  private void executeServiceTaskCommands(Command cmd, ServiceTask modelElementById)
      throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final ServiceTask serviceTask = modelElementById;

    final ZeebeTaskDefinition zeebeTaskDefinition =
        serviceTask
            .getExtensionElements()
            .getElementsQuery()
            .filterByType(ZeebeTaskDefinition.class)
            .singleResult();
    final String taskType = zeebeTaskDefinition.getType();

    JobWorker jobWorker = null;
    try {
      jobWorker =
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
    } finally {
      if (jobWorker != null) {
        jobWorker.close();
      }
    }
  }

  @Override
  public void close() throws Exception {
    zeebeClient.close();
    broker.close();

    Files.walk(new File(tempFolder).toPath())
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }
}
