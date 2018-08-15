package io.zeebe.workbench;

import io.zeebe.workbench.TestCase;

import java.util.List;

public interface Runner {

  public List<TestResult> run(List<WorkflowResource> resources, List<TestCase> cases);
}
