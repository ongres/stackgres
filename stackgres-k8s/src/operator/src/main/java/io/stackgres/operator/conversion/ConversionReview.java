/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.GroupVersionKind;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ConversionReview extends GroupVersionKind {

  private static final long serialVersionUID = 1L;

  private ConversionRequest request;

  public ConversionRequest getRequest() {
    return request;
  }

  public void setRequest(ConversionRequest request) {
    this.request = request;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ConversionReview that = (ConversionReview) o;
    return Objects.equals(request, that.request);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), request);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("request", request)
        .toString();
  }
}
