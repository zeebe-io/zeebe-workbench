package io.zeebe.workbench;

public class Verification {

  private final String expectedIntent;

  private final String expectedPayload;

  private final String activityId;

  public Verification(String expectedIntent, String expectedPayload, String activityId) {
    this.expectedIntent = expectedIntent;
    this.expectedPayload = expectedPayload;
    this.activityId = activityId;
  }

  public String getExpectedIntent() {
    return expectedIntent;
  }

  public String getExpectedPayload() {
    return expectedPayload;
  }

  public String getActivityId() {
    return activityId;
  }
}
