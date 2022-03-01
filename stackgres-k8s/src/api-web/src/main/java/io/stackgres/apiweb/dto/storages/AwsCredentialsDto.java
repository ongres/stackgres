/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class AwsCredentialsDto {

  @JsonProperty("accessKeyId")
  private String accessKey;

  @JsonProperty("secretAccessKey")
  private String secretKey;

  @JsonProperty("secretKeySelectors")
  @Valid
  private AwsSecretKeySelector secretKeySelectors = new AwsSecretKeySelector();

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

  public AwsSecretKeySelector getSecretKeySelectors() {
    return secretKeySelectors;
  }

  public void setSecretKeySelectors(AwsSecretKeySelector secretKeySelectors) {
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
    AwsCredentialsDto that = (AwsCredentialsDto) o;
    return Objects.equals(accessKey, that.accessKey)
        && Objects.equals(secretKey, that.secretKey)
        && Objects.equals(secretKeySelectors, that.secretKeySelectors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessKey, secretKey, secretKeySelectors);
  }
}
