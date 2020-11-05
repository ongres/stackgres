/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class GoogleCloudCredentials {

  @JsonProperty("fetchCredentialsFromMetadataService")
  private boolean fetchCredentialsFromMetadataService;

  @JsonProperty("serviceAccountJSON")
  private String serviceAccountJsonKey;

  @JsonProperty("secretKeySelectors")
  @NotNull(message = "The secretKeySelectors are required")
  @Valid
  private GoogleCloudSecretKeySelector secretKeySelectors;

  public boolean isFetchCredentialsFromMetadataService() {
    return fetchCredentialsFromMetadataService;
  }

  public void setFetchCredentialsFromMetadataService(boolean fetchCredentialsFromMetadataService) {
    this.fetchCredentialsFromMetadataService = fetchCredentialsFromMetadataService;
  }

  public String getServiceAccountJsonKey() {
    return serviceAccountJsonKey;
  }

  public void setServiceAccountJsonKey(String serviceAccountJsonKey) {
    this.serviceAccountJsonKey = serviceAccountJsonKey;
  }

  public GoogleCloudSecretKeySelector getSecretKeySelectors() {
    return secretKeySelectors;
  }

  public void setSecretKeySelectors(GoogleCloudSecretKeySelector secretKeySelectors) {
    this.secretKeySelectors = secretKeySelectors;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
