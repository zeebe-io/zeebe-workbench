package io.zeebe.workbench;

import java.util.List;

public class TestResult {

  private final String name;
  private final List<FailedVerification> failedVerifications;

  public TestResult(String name, List<FailedVerification> failedVerifications) {
    this.name = name;
    this.failedVerifications = failedVerifications;
  }

  public String getName() {
    return name;
  }

  public List<FailedVerification> getFailedVerifications() {
    return failedVerifications;
  }
}
