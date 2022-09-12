/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class GoogleCloudSecretKeySelectorDto {

  @JsonProperty("serviceAccountJSON")
  private SecretKeySelector serviceAccountJsonKey;

  public SecretKeySelector getServiceAccountJsonKey() {
    return serviceAccountJsonKey;
  }

  public void setServiceAccountJsonKey(SecretKeySelector serviceAccountJsonKey) {
    this.serviceAccountJsonKey = serviceAccountJsonKey;
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
    GoogleCloudSecretKeySelectorDto that = (GoogleCloudSecretKeySelectorDto) o;
    return Objects.equals(serviceAccountJsonKey, that.serviceAccountJsonKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceAccountJsonKey);
  }
}
