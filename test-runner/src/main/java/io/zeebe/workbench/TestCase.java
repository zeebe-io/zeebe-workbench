package io.zeebe.workbench;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class TestCase {

  private final String name;
  private final String resourceName;

  private final String startPayload;
  private final List<Command> commands;
  private final List<Verification> verifications;

  public TestCase(
      String name,
      String resourceName,
      String startPayload,
      List<Command> commands,
      List<Verification> verifications) {
    this.name = name;
    this.resourceName = resourceName;
    this.startPayload = startPayload;
    this.commands = commands;
    this.verifications = verifications;
  }

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public TestCase(
      @JsonProperty("name") String name,
      @JsonProperty("resourceName") String resourceName,
      @JsonProperty("startPayload") JsonNode startPayload,
      @JsonProperty("commands") List<Command> commands,
      @JsonProperty("verifications") List<Verification> verifications) {
    this.name = name;
    this.resourceName = resourceName;
    this.startPayload = startPayload.toString();
    this.commands = commands;
    this.verifications = verifications;
  }

  public String getName() {
    return name;
  }

  public String getResourceName() {
    return resourceName;
  }

  public String getStartPayload() {
    return startPayload;
  }

  public List<Command> getCommands() {
    return commands;
  }

  public List<Verification> getVerifications() {
    return verifications;
  }
}
