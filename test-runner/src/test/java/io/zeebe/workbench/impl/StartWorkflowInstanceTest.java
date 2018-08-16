package io.zeebe.workbench.impl;

import io.zeebe.gateway.ZeebeClient;
import io.zeebe.gateway.api.clients.TopicClient;
import io.zeebe.gateway.api.commands.WorkflowInstanceCommand;
import io.zeebe.gateway.api.commands.WorkflowInstanceCommandName;
import io.zeebe.gateway.api.events.WorkflowInstanceEvent;
import io.zeebe.gateway.api.events.WorkflowInstanceState;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.test.util.AutoCloseableRule;
import io.zeebe.workbench.TestCase;
import io.zeebe.workbench.WorkflowResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class StartWorkflowInstanceTest {

  @Rule public AutoCloseableRule closeableRule = new AutoCloseableRule();

  private final TestRunner runner = new TestRunner();

  private WorkflowResource resource;

  public static final BpmnModelInstance PROCESS =
      Bpmn.createExecutableProcess("PROCESS")
          .startEvent()
          .serviceTask("id", serviceTaskBuilder -> serviceTaskBuilder.zeebeTaskType("type"))
          .endEvent()
          .done();

  @Before
  public void setup() {
    closeableRule.manage(runner);
    resource = new WorkflowResource(Bpmn.convertToString(PROCESS).getBytes(), "process.bpmn");
  }

  @Test
  public void shouldStartWorkflowInstance() throws Exception {
    // given
    final TestCase testCase = new TestCase("test1", "process.bpmn", (String) null, null, null);

    // when
    runner.run(resource, testCase);

    // then
    final CountDownLatch latch = new CountDownLatch(8);

    final List<WorkflowInstanceEvent> events = new CopyOnWriteArrayList<>();
    final List<WorkflowInstanceCommand> commands = new CopyOnWriteArrayList<>();

    final ZeebeClient zeebeClient = ZeebeClient.newClient();
    final TopicClient topicClient = zeebeClient.topicClient();
    topicClient
        .newSubscription()
        .name("subscription")
        .workflowInstanceEventHandler(
            workflowInstanceEvent -> {
              events.add(workflowInstanceEvent);
              latch.countDown();
            })
        .workflowInstanceCommandHandler(
            command -> {
              commands.add(command);
              latch.countDown();
            })
        .open();

    latch.await(15, TimeUnit.SECONDS);

    assertThat(commands.size()).isEqualTo(1);
    assertThat(commands.stream().map(e -> e.getName()))
        .containsExactly(WorkflowInstanceCommandName.CREATE);

    assertThat(events.size()).isEqualTo(7);
    assertThat(events.stream().map(e -> e.getState()))
        .containsExactly(
            WorkflowInstanceState.CREATED,
            WorkflowInstanceState.ELEMENT_READY,
            WorkflowInstanceState.ELEMENT_ACTIVATED,
            WorkflowInstanceState.START_EVENT_OCCURRED,
            WorkflowInstanceState.SEQUENCE_FLOW_TAKEN,
            WorkflowInstanceState.ELEMENT_READY,
            WorkflowInstanceState.ELEMENT_ACTIVATED);
  }

  @Test
  public void shouldStartWorkflowInstanceWithPayload() throws Exception {
    // given
    final String startPayload = "{\"foo\":3}";
    final TestCase testCase = new TestCase("test1", "process.bpmn", startPayload, null, null);

    // when
    runner.run(resource, testCase);

    // then
    final CountDownLatch latch = new CountDownLatch(1);

    final List<WorkflowInstanceEvent> events = new CopyOnWriteArrayList<>();

    final ZeebeClient zeebeClient = ZeebeClient.newClient();
    final TopicClient topicClient = zeebeClient.topicClient();
    topicClient
        .newSubscription()
        .name("subscription1")
        .workflowInstanceEventHandler(
            workflowInstanceEvent -> {
              events.add(workflowInstanceEvent);
              latch.countDown();
            })
        .open();

    latch.await(15, TimeUnit.SECONDS);

    final WorkflowInstanceEvent workflowInstanceEvent = events.get(0);

    assertThat(workflowInstanceEvent.getState()).isEqualTo(WorkflowInstanceState.CREATED);
    assertThat(workflowInstanceEvent.getPayload()).isEqualTo(startPayload);
  }
}
