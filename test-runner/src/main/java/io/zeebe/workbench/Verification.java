package io.zeebe.workbench;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class Verification {

  private final String expectedIntent;

  private final String expectedPayload;

  private final String activityId;

  public Verification(String expectedIntent, String expectedPayload, String activityId) {
    this.expectedIntent = expectedIntent;
    this.expectedPayload = expectedPayload;
    this.activityId = activityId;
  }

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public Verification(
      @JsonProperty("expectedIntent") String expectedIntent,
      @JsonProperty("expectedPayload") JsonNode expectedPayload,
      @JsonProperty("activityId") String activityId) {
    this.expectedIntent = expectedIntent;
    this.expectedPayload = expectedPayload.toString();
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
