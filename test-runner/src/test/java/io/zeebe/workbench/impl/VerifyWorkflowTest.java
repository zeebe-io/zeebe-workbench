package io.zeebe.workbench.impl;

import io.zeebe.gateway.api.events.WorkflowInstanceState;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.test.util.AutoCloseableRule;
import io.zeebe.workbench.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VerifyWorkflowTest {

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
  public void shouldVerifyWorkflowEnd() {
    // given
    final String startPayload = "{\"foo\":3}";
    final String completePayload = "{\"bar\":-1}";

    final Command command = new Command("serviceTask", completePayload);
    final Verification verification =
        new Verification(
            WorkflowInstanceState.ELEMENT_COMPLETED.name(), "{\"foo\":3, \"bar\":-1}", "PROCESS");
    final TestCase testCase =
        new TestCase(
            "test1",
            "process.bpmn",
            startPayload,
            Collections.singletonList(command),
            Collections.singletonList(verification));

    // when
    final List<TestResult> results = runner.run(resource, testCase);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  public void shouldReturnFailedVerificationIfWorkflowNotEnd() {
    // given
    final String startPayload = "{\"foo\":3}";

    final Verification verification =
        new Verification(
            WorkflowInstanceState.ELEMENT_COMPLETED.name(), "{\"foo\":3, \"bar\":-1}", "PROCESS");
    final TestCase testCase =
        new TestCase(
            "test1", "process.bpmn", startPayload, null, Collections.singletonList(verification));

    // when
    final List<TestResult> results = runner.run(resource, testCase);

    // then
    assertThat(results.size()).isEqualTo(1);
    assertThat(
            results
                .stream()
                .flatMap(r -> r.getFailedVerifications().stream())
                .map(v -> v.getVerification().getActivityId()))
        .containsExactly("PROCESS");
  }
}
