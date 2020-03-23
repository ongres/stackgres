/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.dto.SecretKeySelector;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class GoogleCloudCredentials {

  @JsonProperty("serviceAccountJsonKey")
  private String serviceAccountJsonKey;

  @JsonProperty("serviceAccountJsonKeySelector")
  private SecretKeySelector serviceAccountJsonKeySelector;

  public String getServiceAccountJsonKey() {
    return serviceAccountJsonKey;
  }

  public void setServiceAccountJsonKey(String serviceAccountJsonKey) {
    this.serviceAccountJsonKey = serviceAccountJsonKey;
  }

  public SecretKeySelector getServiceAccountJsonKeySelector() {
    return serviceAccountJsonKeySelector;
  }

  public void setServiceAccountJsonKeySelector(SecretKeySelector serviceAccountJsonKeySelector) {
    this.serviceAccountJsonKeySelector = serviceAccountJsonKeySelector;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("serviceAccountJsonKey", serviceAccountJsonKey)
        .add("serviceAccountJsonKeySelector", serviceAccountJsonKeySelector)
        .toString();
  }

}
