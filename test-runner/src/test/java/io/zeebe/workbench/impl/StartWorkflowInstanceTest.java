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

import java.util.ArrayList;
import java.util.List;
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
  public void shouldDeployWorkflow() throws Exception {
    // given
    final TestCase testCase = new TestCase("test1", "PROCESS", null, null, null);

    // when
    runner.run(resource, testCase);

    // then
    final CountDownLatch latch = new CountDownLatch(8);

    final List<WorkflowInstanceEvent> events = new ArrayList<>();
    final List<WorkflowInstanceCommand> commands = new ArrayList<>();

    final ZeebeClient zeebeClient = ZeebeClient.newClient();
    final TopicClient topicClient = zeebeClient.topicClient();
    topicClient
        .newSubscription()
        .name("subscription")
        .workflowInstanceEventHandler(
            workflowInstanceEvent -> {
              latch.countDown();
              events.add(workflowInstanceEvent);
            })
        .workflowInstanceCommandHandler(
            command -> {
              latch.countDown();
              commands.add(command);
            })
        .open();

    latch.await(15, TimeUnit.SECONDS);

    assertThat(commands.size()).isEqualTo(1);
    assertThat(commands.stream().map(e -> e.getName()))
        .containsExactly(WorkflowInstanceCommandName.CREATE);

    assertThat(events.size()).isEqualTo(7);
    assertThat(events.stream().map(e -> e.getState()))
        .contains(
            WorkflowInstanceState.CREATED,
            WorkflowInstanceState.ELEMENT_READY,
            WorkflowInstanceState.ELEMENT_ACTIVATED,
            WorkflowInstanceState.START_EVENT_OCCURRED,
            WorkflowInstanceState.SEQUENCE_FLOW_TAKEN,
            WorkflowInstanceState.ELEMENT_READY,
            WorkflowInstanceState.ELEMENT_ACTIVATED);
  }
}
