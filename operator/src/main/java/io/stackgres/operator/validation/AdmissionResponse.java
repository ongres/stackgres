package io.stackgres.operator.validation;

import java.util.UUID;

public class AdmissionResponse {

  private UUID uid;

  private boolean allowed;

  private Result status;

  public UUID getUid() {
    return uid;
  }

  public void setUid(UUID uid) {
    this.uid = uid;
  }

  public boolean isAllowed() {
    return allowed;
  }

  public void setAllowed(boolean allowed) {
    this.allowed = allowed;
  }

  public Result getStatus() {
    return status;
  }

  public void setStatus(Result status) {
    this.status = status;
  }
}
