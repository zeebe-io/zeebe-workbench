package io.zeebe.workbench;

import java.util.ArrayList;
import java.util.List;

public class TestResult {

  private final String name;
  private final List<FailedVerification> failedVerifications = new ArrayList<>();

  public TestResult(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public List<FailedVerification> getFailedVerifications() {
    return failedVerifications;
  }

  public void addFailedVerfifications(List<FailedVerification> failedVerifications) {
    this.failedVerifications.addAll(failedVerifications);
  }
}
