/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import java.util.Objects;

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
public class GoogleCloudCredentialsDto {

  @JsonProperty("fetchCredentialsFromMetadataService")
  private boolean fetchCredentialsFromMetadataService;

  @JsonProperty("serviceAccountJSON")
  private String serviceAccountJsonKey;

  @JsonProperty("secretKeySelectors")
  @NotNull(message = "The secretKeySelectors are required")
  @Valid
  private GoogleCloudSecretKeySelectorDto secretKeySelectors;

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

  public GoogleCloudSecretKeySelectorDto getSecretKeySelectors() {
    return secretKeySelectors;
  }

  public void setSecretKeySelectors(GoogleCloudSecretKeySelectorDto secretKeySelectors) {
    this.secretKeySelectors = secretKeySelectors;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GoogleCloudCredentialsDto that = (GoogleCloudCredentialsDto) o;
    return fetchCredentialsFromMetadataService == that.fetchCredentialsFromMetadataService
        && Objects.equals(serviceAccountJsonKey, that.serviceAccountJsonKey)
        && Objects.equals(secretKeySelectors, that.secretKeySelectors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fetchCredentialsFromMetadataService, serviceAccountJsonKey,
        secretKeySelectors);
  }
}
