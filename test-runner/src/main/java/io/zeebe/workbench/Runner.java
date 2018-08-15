package io.zeebe.workbench;

import java.util.List;

public interface Runner {

  public List<TestResult> run(WorkflowResource resources, TestCase cases);

  public List<TestResult> run(List<WorkflowResource> resources, List<TestCase> cases);
}
