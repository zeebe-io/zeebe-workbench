package io.zeebe.workbench;

import java.util.List;

public class TestCase {

  private final String name;
  private final String resourceName;

  private final String startPayload;
  private final List<Command> commands;
  private final List<Verification> verifications;

  public TestCase(String name, String resourceName, String startPayload, List<Command> commands, List<Verification> verfications) {
    this.name = name;
    this.resourceName = resourceName;
    this.startPayload = startPayload;
    this.commands = commands;
    this.verifications = verfications;
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
