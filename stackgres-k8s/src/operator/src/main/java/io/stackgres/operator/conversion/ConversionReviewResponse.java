/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ConversionReviewResponse {

  private String kind = "ConversionReview";

  private String apiVersion = "apiextensions.k8s.io/v1";

  public ConversionReviewResponse() {
  }

  public ConversionReviewResponse(ConversionResponse response) {
    this.response = response;
  }

  private ConversionResponse response;

  public ConversionResponse getResponse() {
    return response;
  }

  public void setResponse(ConversionResponse response) {
    this.response = response;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConversionReviewResponse that = (ConversionReviewResponse) o;
    return Objects.equals(kind, that.kind)
        && Objects.equals(apiVersion, that.apiVersion)
        && Objects.equals(response, that.response);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, apiVersion, response);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("kind", kind)
        .add("apiVersion", apiVersion)
        .add("response", response)
        .toString();
  }
}
