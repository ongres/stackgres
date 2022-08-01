/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class GoogleCloudSecretKeySelector {

  @JsonProperty("serviceAccountJSON")
  @NotNull(message = "The serviceAccountJsonKey is required")
  @Valid
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
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GoogleCloudSecretKeySelector that = (GoogleCloudSecretKeySelector) o;
    return Objects.equals(serviceAccountJsonKey, that.serviceAccountJsonKey);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
