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
import io.zeebe.workbench.Command;
import io.zeebe.workbench.TestCase;
import io.zeebe.workbench.WorkflowResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class CompleteJobCommandTest {

  @Rule public AutoCloseableRule closeableRule = new AutoCloseableRule();

  private final TestRunner runner = new TestRunner();

  private WorkflowResource resource;

  public static final BpmnModelInstance PROCESS =
      Bpmn.createExecutableProcess("PROCESS")
          .startEvent()
          .serviceTask(
              "serviceTask", serviceTaskBuilder -> serviceTaskBuilder.zeebeTaskType("type"))
          .endEvent()
          .done();

  @Before
  public void setup() {
    closeableRule.manage(runner);
    resource = new WorkflowResource(Bpmn.convertToString(PROCESS).getBytes(), "process.bpmn");
  }

  @Test
  public void shouldCompleteJob() throws Exception {
    // given
    final String startPayload = "{\"foo\":3}";
    final Command command = new Command("serviceTask", (String) null);
    final TestCase testCase =
        new TestCase(
            "test1", "process.bpmn", startPayload, Collections.singletonList(command), null);

    // when
    runner.run(resource, testCase);

    // then
    final CountDownLatch latch = new CountDownLatch(1);

    final List<WorkflowInstanceEvent> events = new CopyOnWriteArrayList<>();
    final List<WorkflowInstanceCommand> commands = new CopyOnWriteArrayList<>();

    final ZeebeClient zeebeClient = ZeebeClient.newClient();
    final TopicClient topicClient = zeebeClient.topicClient();
    topicClient
        .newSubscription()
        .name("subscription2")
        .workflowInstanceEventHandler(
            workflowInstanceEvent -> {
              events.add(workflowInstanceEvent);

              if (workflowInstanceEvent.getState() == WorkflowInstanceState.ELEMENT_COMPLETED
                  && workflowInstanceEvent
                      .getActivityId()
                      .equals(workflowInstanceEvent.getBpmnProcessId())) {
                latch.countDown();
              }
            })
        .workflowInstanceCommandHandler(
            c -> {
              commands.add(c);
            })
        .open();

    latch.await(5, TimeUnit.SECONDS);

    assertThat(commands.size()).isEqualTo(1);
    assertThat(commands.stream().map(e -> e.getName()))
        .containsExactly(WorkflowInstanceCommandName.CREATE);

    assertThat(events.size()).isEqualTo(13);
    assertThat(events.stream().map(e -> e.getState()))
        .containsExactly(
            WorkflowInstanceState.CREATED,
            WorkflowInstanceState.ELEMENT_READY,
            WorkflowInstanceState.ELEMENT_ACTIVATED,
            WorkflowInstanceState.START_EVENT_OCCURRED,
            WorkflowInstanceState.SEQUENCE_FLOW_TAKEN,
            WorkflowInstanceState.ELEMENT_READY,
            WorkflowInstanceState.ELEMENT_ACTIVATED,
            WorkflowInstanceState.ELEMENT_COMPLETING,
            WorkflowInstanceState.ELEMENT_COMPLETED,
            WorkflowInstanceState.SEQUENCE_FLOW_TAKEN,
            WorkflowInstanceState.END_EVENT_OCCURRED,
            WorkflowInstanceState.ELEMENT_COMPLETING,
            WorkflowInstanceState.ELEMENT_COMPLETED);
  }

  @Test
  public void shouldCompleteJobWithPayload() throws Exception {
    // given
    final String startPayload = "{\"foo\":3}";
    final String completePayload = "{\"bar\":-1}";

    final Command command = new Command("serviceTask", completePayload);
    final TestCase testCase =
        new TestCase(
            "test1", "process.bpmn", startPayload, Collections.singletonList(command), null);

    // when
    runner.run(resource, testCase);

    // then
    final CountDownLatch latch = new CountDownLatch(1);

    final List<WorkflowInstanceEvent> events = new CopyOnWriteArrayList<>();

    final ZeebeClient zeebeClient = ZeebeClient.newClient();
    final TopicClient topicClient = zeebeClient.topicClient();
    topicClient
        .newSubscription()
        .name("subscription3")
        .workflowInstanceEventHandler(
            workflowInstanceEvent -> {
              events.add(workflowInstanceEvent);

              if (workflowInstanceEvent.getState() == WorkflowInstanceState.ELEMENT_COMPLETED
                  && workflowInstanceEvent
                      .getActivityId()
                      .equals(workflowInstanceEvent.getBpmnProcessId())) {
                latch.countDown();
              }
            })
        .open();

    latch.await(5, TimeUnit.SECONDS);

    final WorkflowInstanceEvent workflowInstanceEvent = events.get(events.size() - 1);
    assertThat(workflowInstanceEvent.getState()).isEqualTo(WorkflowInstanceState.ELEMENT_COMPLETED);
    assertThat(workflowInstanceEvent.getPayloadAsMap())
        .containsExactly(entry("foo", 3), entry("bar", -1));
  }
}
