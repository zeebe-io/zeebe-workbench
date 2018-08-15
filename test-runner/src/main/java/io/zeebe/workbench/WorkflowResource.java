package io.zeebe.workbench;

public class WorkflowResource {

  private final byte[] resource;
  private final String name;

  public WorkflowResource(byte[] resource, String name) {
    this.resource = resource;
    this.name = name;
  }

  public byte[] getResource() {
    return resource;
  }

  public String getName() {
    return name;
  }
}
