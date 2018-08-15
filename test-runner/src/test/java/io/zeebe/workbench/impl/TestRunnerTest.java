package io.zeebe.workbench.impl;

import io.zeebe.gateway.ZeebeClient;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.workbench.WorkflowResource;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRunnerTest {

  private final TestRunner runner = new TestRunner();

  @Test
  public void shouldDeployWorkflow() throws Exception {
    final List<WorkflowResource> resourceList = new ArrayList<>();

    final BpmnModelInstance modelInstance =
        Bpmn.createExecutableProcess("PROCESS")
            .startEvent()
            .serviceTask("id", serviceTaskBuilder -> serviceTaskBuilder.zeebeTaskType("type"))
            .endEvent()
            .done();

    resourceList.add(
        new WorkflowResource(Bpmn.convertToString(modelInstance).getBytes(), "process.bpmn"));

    runner.run(resourceList, null);

    final ZeebeClient zeebeClient = ZeebeClient.newClient();
    final io.zeebe.gateway.api.commands.WorkflowResource resource =
        zeebeClient
            .topicClient()
            .workflowClient()
            .newResourceRequest()
            .bpmnProcessId("PROCESS")
            .latestVersion()
            .send()
            .join();

    assertThat(resource.getBpmnXml()).isEqualTo(Bpmn.convertToString(modelInstance));
  }
}
