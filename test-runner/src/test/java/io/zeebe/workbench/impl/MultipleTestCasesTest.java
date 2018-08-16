package io.zeebe.workbench.impl;

import io.zeebe.gateway.api.events.WorkflowInstanceState;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.test.util.AutoCloseableRule;
import io.zeebe.workbench.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipleTestCasesTest {

  @Rule public AutoCloseableRule closeableRule = new AutoCloseableRule();

  private final TestRunner runner = new TestRunner();

  private List<WorkflowResource> resources = new ArrayList<>();

  public static final BpmnModelInstance PROCESS =
      Bpmn.createExecutableProcess("PROCESS")
          .startEvent()
          .serviceTask(
              "serviceTask", serviceTaskBuilder -> serviceTaskBuilder.zeebeTaskType("type"))
          .endEvent()
          .done();

  public static final BpmnModelInstance PROCESS_2 =
      Bpmn.createExecutableProcess("PROCESS_2")
          .startEvent()
          .serviceTask(
              "serviceTask", serviceTaskBuilder -> serviceTaskBuilder.zeebeTaskType("type_2"))
          .endEvent()
          .done();

  @Before
  public void setup() {
    closeableRule.manage(runner);
    resources.add(new WorkflowResource(Bpmn.convertToString(PROCESS).getBytes(), "process.bpmn"));
    resources.add(
        new WorkflowResource(Bpmn.convertToString(PROCESS_2).getBytes(), "process_2.bpmn"));
  }

  @Test
  public void shouldRunMultipleTestCases() {
    // given
    final List<TestCase> testCases = new ArrayList<>();

    testCases.add(createTestCase("test-1", "PROCESS", "process.bpmn"));
    testCases.add(createTestCase("test-2", "PROCESS_2", "process_2.bpmn"));

    // when
    final List<TestResult> results = runner.run(resources, testCases);

    // then
    assertThat(results.stream().flatMap(r -> r.getFailedVerifications().stream())).isEmpty();
  }

  @Test
  public void shouldRunMultipleTestCasesTwice() {
    // given
    final List<TestCase> testCases = new ArrayList<>();

    testCases.add(createTestCase("test-1", "PROCESS", "process.bpmn"));
    testCases.add(createTestCase("test-2", "PROCESS_2", "process_2.bpmn"));

    // when
    List<TestResult> results = runner.run(resources, testCases);

    // then
    assertThat(results.stream().flatMap(r -> r.getFailedVerifications().stream())).isEmpty();

    // when
    results = runner.run(resources, testCases);

    // then
    assertThat(results.stream().flatMap(r -> r.getFailedVerifications().stream())).isEmpty();
  }

  @Test
  public void shouldRunMultipleTestCasesOnSameResource() {
    // given
    final List<TestCase> testCases = new ArrayList<>();

    testCases.add(createTestCase("test-1", "PROCESS", "process.bpmn"));

    final String startPayload = "{\"foo\":3}";
    final Verification verification =
        new Verification(
            WorkflowInstanceState.ELEMENT_COMPLETED.name(), "{\"foo\":3, \"bar\":-1}", "PROCESS");
    final TestCase failing =
        new TestCase(
            "test-2", "process.bpmn", startPayload, null, Collections.singletonList(verification));
    testCases.add(failing);

    // when
    final List<TestResult> results = runner.run(resources, testCases);

    // then
    final TestResult result = results.get(0);
    assertThat(result.getFailedVerifications()).isEmpty();

    final TestResult failedTest = results.get(1);
    assertThat(failedTest.getFailedVerifications().size()).isEqualTo(1);
    final Verification failedVerification =
        failedTest.getFailedVerifications().get(0).getVerification();
    assertThat(failedVerification.getActivityId()).isEqualTo("PROCESS");
    assertThat(failedVerification.getExpectedIntent()).isEqualTo("ELEMENT_COMPLETED");
  }

  private TestCase createTestCase(String id, String bpmnProcessId, String processName) {
    final String startPayload = "{\"foo\":3}";
    final String completePayload = "{\"bar\":-1}";

    final Command command = new Command("serviceTask", completePayload);
    final Verification verification =
        new Verification(
            WorkflowInstanceState.ELEMENT_COMPLETED.name(),
            "{\"foo\":3, \"bar\":-1}",
            bpmnProcessId);
    return new TestCase(
        id,
        processName,
        startPayload,
        Collections.singletonList(command),
        Collections.singletonList(verification));
  }
}
