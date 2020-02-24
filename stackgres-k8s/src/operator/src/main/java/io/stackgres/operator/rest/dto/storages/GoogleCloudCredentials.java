/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.storages;

import java.util.Objects;

import javax.validation.constraints.NotNull;

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
  @NotNull(message = "The serviceAccountJsonKey is required")
  private SecretKeySelector serviceAccountJsonKey;

  public SecretKeySelector getServiceAccountJsonKey() {
    return serviceAccountJsonKey;
  }

  public void setServiceAccountJsonKey(SecretKeySelector serviceAccountJsonKey) {
    this.serviceAccountJsonKey = serviceAccountJsonKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceAccountJsonKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof GoogleCloudCredentials)) {
      return false;
    }
    GoogleCloudCredentials other = (GoogleCloudCredentials) obj;
    return Objects.equals(serviceAccountJsonKey, other.serviceAccountJsonKey);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("serviceAccountJsonKey", serviceAccountJsonKey)
        .toString();
  }

}
