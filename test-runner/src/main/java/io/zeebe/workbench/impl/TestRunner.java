package io.zeebe.workbench.impl;

import io.zeebe.workbench.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestRunner implements Runner {

  private final HashMap<String, WorkflowResource> deployedResources = new HashMap<>();

  @Override
  public List<TestResult> run(List<WorkflowResource> resources, List<TestCase> cases) {
    deploy(resources);

    return runTests(cases);
  }

  private void deploy(List<WorkflowResource> resources)
  {
    for (WorkflowResource resource : resources)
    {
      deployedResources.put(resource.getName(), resource);

      // TODO deploy on broker
    }
  }

  private List<TestResult> runTests(List<TestCase> cases)
  {
    final List<TestResult> results = new ArrayList<>();
    for (TestCase testCase : cases)
    {
      runTest(testCase);
    }
    return results;
  }

  private TestResult runTest(TestCase testCase) {
    final TestResult result = new TestResult(testCase.getName());

    final String resourceName = testCase.getResourceName();
    final WorkflowResource resource = deployedResources.get(resourceName);
    final String startPayload = testCase.getStartPayload();

    // TODO start workflow instance with given payLoad

    final List<Command> commands = testCase.getCommands();

    // TODO run complete job commands

    // TODO verify current state
    final List<Verification> verifications = testCase.getVerifications();
    for (Verification verification : verifications)
    {
      // TODO verify expected state
      // TODO add failed verification on failed verification
    }

    return result;
  }


}
