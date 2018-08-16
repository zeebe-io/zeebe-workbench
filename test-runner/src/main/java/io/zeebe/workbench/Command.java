package io.zeebe.workbench;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class Command {

  private final String activityId;
  private final String payload;

  public Command(String activityId, String payload) {
    this.activityId = activityId;
    this.payload = payload;
  }

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public Command(
      @JsonProperty("activityId") String activityId, @JsonProperty("payload") JsonNode payload) {
    this.activityId = activityId;
    this.payload = payload.toString();
  }

  public String getActivityId() {
    return activityId;
  }

  public String getPayload() {
    return payload;
  }
}
