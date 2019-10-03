/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import io.fabric8.kubernetes.api.model.GroupVersionKind;

public abstract class AdmissionReview<T> extends GroupVersionKind {

  private static final long serialVersionUID = -7649295266069293729L;

  private AdmissionRequest<T> request;

  public AdmissionRequest<T> getRequest() {
    return request;
  }

  public void setRequest(AdmissionRequest<T> request) {
    this.request = request;
  }
}
