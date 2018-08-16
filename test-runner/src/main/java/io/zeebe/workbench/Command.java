package io.zeebe.workbench;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Command {

  private final String activityId;
  private final String payload;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public Command(
      @JsonProperty("activityId") String activityId, @JsonProperty("payload") String payload) {
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
