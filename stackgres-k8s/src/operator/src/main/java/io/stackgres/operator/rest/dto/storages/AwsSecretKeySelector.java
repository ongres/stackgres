/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.dto.SecretKeySelector;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class AwsSecretKeySelector {

  @JsonProperty("accessKeyId")
  private SecretKeySelector accessKeySelector;

  @JsonProperty("secretAccessKey")
  private SecretKeySelector secretKeySelector;

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
}
