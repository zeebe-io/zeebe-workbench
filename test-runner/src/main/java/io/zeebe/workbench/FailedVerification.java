package io.zeebe.workbench;

public class FailedVerification {

  private final Verification verification;
  private final String actualPayLoad;

  public FailedVerification(Verification verification, String actualPayLoad) {
    this.verification = verification;
    this.actualPayLoad = actualPayLoad;
  }

  public Verification getVerification() {
    return verification;
  }

  public String getActualPayLoad() {
    return actualPayLoad;
  }
}
