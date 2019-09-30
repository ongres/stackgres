/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import io.fabric8.kubernetes.api.model.GroupVersionKind;

public class AdmissionReview extends GroupVersionKind {

  private static final long serialVersionUID = -7649295266069293729L;

  private AdmissionRequest request;

  public AdmissionRequest getRequest() {
    return request;
  }

  public void setRequest(AdmissionRequest request) {
    this.request = request;
  }
}
