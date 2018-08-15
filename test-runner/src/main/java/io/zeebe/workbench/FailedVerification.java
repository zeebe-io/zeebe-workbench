package io.zeebe.workbench;

public class FailedVerification {

  private final String expectedIntent;

  private final String expectedPayload;
  private final String actualPayLoad;

  private final String activityId;

  public FailedVerification(String expectedIntent, String expectedPayload, String actualPayLoad, String activityId) {
    this.expectedIntent = expectedIntent;
    this.expectedPayload = expectedPayload;
    this.actualPayLoad = actualPayLoad;
    this.activityId = activityId;
  }

  public String getExpectedIntent() {
    return expectedIntent;
  }

  public String getExpectedPayload() {
    return expectedPayload;
  }

  public String getActualPayLoad() {
    return actualPayLoad;
  }

  public String getActivityId() {
    return activityId;
  }
}
