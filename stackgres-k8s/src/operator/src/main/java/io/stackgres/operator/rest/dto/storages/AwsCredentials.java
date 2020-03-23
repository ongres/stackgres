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
public class AwsCredentials {

  @JsonProperty("accessKey")
  private String accessKey;

  @JsonProperty("secretKey")
  private String secretKey;

  @JsonProperty("accessKeySelector")
  private SecretKeySelector accessKeySelector;

  @JsonProperty("secretKeySelector")
  private SecretKeySelector secretKeySelector;

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public SecretKeySelector getAccessKeySelector() {
    return accessKeySelector;
  }

  public void setAccessKeySelector(SecretKeySelector accessKeySelector) {
    this.accessKeySelector = accessKeySelector;
  }

  public SecretKeySelector getSecretKeySelector() {
    return secretKeySelector;
  }

  public void setSecretKeySelector(SecretKeySelector secretKeySelector) {
    this.secretKeySelector = secretKeySelector;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("accessKey", accessKey)
        .add("secretKey", secretKey)
        .add("accessKeySelector", accessKeySelector)
        .add("secretKeySelector", secretKeySelector)
        .toString();
  }

}
