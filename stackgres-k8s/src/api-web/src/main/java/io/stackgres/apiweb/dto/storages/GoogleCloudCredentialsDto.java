/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class GoogleCloudCredentialsDto {

  private boolean fetchCredentialsFromMetadataService;

  @JsonProperty("serviceAccountJSON")
  private String serviceAccountJsonKey;

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

}
