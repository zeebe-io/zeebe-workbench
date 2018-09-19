package io.zeebe.workbench.impl;

import io.zeebe.gateway.ZeebeClient;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.test.util.AutoCloseableRule;
import io.zeebe.workbench.WorkflowResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DeployWorkflowResourceTest {

  @Rule public AutoCloseableRule closeableRule = new AutoCloseableRule();

  private final TestRunner runner = new TestRunner();

  @Before
  public void setUp() {
    closeableRule.manage(runner);
  }

  @Test
  public void shouldStartWorkflow() throws Exception {
    // given
    final List<WorkflowResource> resourceList = new ArrayList<>();

    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("PROCESS")
            .startEvent()
            .serviceTask("id", serviceTaskBuilder -> serviceTaskBuilder.zeebeTaskType("type"))
            .endEvent()
            .done();

    resourceList.add(
        new WorkflowResource(Bpmn.convertToString(modelInstance).getBytes(), "process.bpmn"));

    // when
    runner.run(resourceList, null);

    // then
    final ZeebeClient zeebeClient = ZeebeClient.newClient();
    final io.zeebe.gateway.api.commands.WorkflowResource resource =
        zeebeClient
            .workflowClient()
            .newResourceRequest()
            .bpmnProcessId("PROCESS")
            .latestVersion()
            .send()
            .join();

    assertThat(resource.getBpmnXml()).isEqualTo(Bpmn.convertToString(modelInstance));
  }
}
