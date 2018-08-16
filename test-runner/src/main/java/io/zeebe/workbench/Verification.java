package io.zeebe.workbench;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Verification {

  private final String expectedIntent;

  private final String expectedPayload;

  private final String activityId;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public Verification(
      @JsonProperty("expectedIntent") String expectedIntent,
      @JsonProperty("expectedPayload") String expectedPayload,
      @JsonProperty("activityId") String activityId) {
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
