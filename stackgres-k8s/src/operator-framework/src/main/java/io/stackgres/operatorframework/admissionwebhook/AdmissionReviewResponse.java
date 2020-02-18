/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook;

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
}
