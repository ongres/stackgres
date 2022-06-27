/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public abstract class AdmissionReview<T extends HasMetadata> {

  @JsonProperty("apiVersion")
  private String apiVersion = "admission.k8s.io/v1";

  @JsonProperty("kind")
  private String kind = "AdmissionReview";

  @JsonProperty("request")
  private AdmissionRequest<T> request;

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public AdmissionRequest<T> getRequest() {
    return request;
  }

  public void setRequest(AdmissionRequest<T> request) {
    this.request = request;
  }

}
