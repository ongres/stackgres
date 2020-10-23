/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook;

import java.util.Objects;

import io.fabric8.kubernetes.api.model.GroupVersionKind;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class AdmissionReviewResponse extends GroupVersionKind {

  private static final long serialVersionUID = -2087380329442965085L;

  private AdmissionResponse response;

  public AdmissionResponse getResponse() {
    return response;
  }

  public void setResponse(AdmissionResponse response) {
    this.response = response;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(response);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof AdmissionReviewResponse)) {
      return false;
    }
    AdmissionReviewResponse other = (AdmissionReviewResponse) obj;
    return Objects.equals(response, other.response);
  }

}
