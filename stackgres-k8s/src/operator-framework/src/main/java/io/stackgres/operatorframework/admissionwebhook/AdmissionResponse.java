/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Status;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdmissionResponse {

  private UUID uid;

  private boolean allowed;

  private Status status;

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

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
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

  @Override
  public int hashCode() {
    return Objects.hash(allowed, patch, patchType, status, uid);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AdmissionResponse)) {
      return false;
    }
    AdmissionResponse other = (AdmissionResponse) obj;
    return allowed == other.allowed
        && Objects.equals(patch, other.patch)
        && Objects.equals(patchType, other.patchType)
        && Objects.equals(status, other.status)
        && Objects.equals(uid, other.uid);
  }

}
