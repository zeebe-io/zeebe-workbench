package io.zeebe.workbench;

public class Command {

  private final String activityId;
  private final String payload;

  public Command(String activityId, String payload) {
    this.activityId = activityId;
    this.payload = payload;
  }

  public String getActivityId() {
    return activityId;
  }

  public String getPayload() {
    return payload;
  }
}
