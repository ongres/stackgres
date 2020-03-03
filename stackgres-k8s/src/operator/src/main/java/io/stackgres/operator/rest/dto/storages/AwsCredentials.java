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
public class AwsCredentials {

  @JsonProperty("accessKey")
  @NotNull(message = "The accessKey is required")
  private SecretKeySelector accessKey;

  @JsonProperty("secretKey")
  @NotNull(message = "The secretKey is required")
  private SecretKeySelector secretKey;

  public SecretKeySelector getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(SecretKeySelector accessKey) {
    this.accessKey = accessKey;
  }

  public SecretKeySelector getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(SecretKeySelector secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessKey, secretKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AwsCredentials)) {
      return false;
    }
    AwsCredentials other = (AwsCredentials) obj;
    return Objects.equals(accessKey, other.accessKey) && Objects.equals(secretKey, other.secretKey);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("accessKey", accessKey)
        .add("secretKey", secretKey)
        .toString();
  }

}
