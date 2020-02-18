/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdmissionResponse {

  private UUID uid;

  private boolean allowed;

  private Result status;

  private String patchType;

  private String patch;

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

  public String getPatchType() {
    return patchType;
  }

  public void setPatchType(String patchType) {
    this.patchType = patchType;
  }

  public String getPatch() {
    return patch;
  }

  public void setPatch(String patch) {
    this.patch = patch;
  }
}
