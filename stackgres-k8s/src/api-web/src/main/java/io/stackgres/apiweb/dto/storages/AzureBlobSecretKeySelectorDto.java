/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class AzureBlobSecretKeySelectorDto {

  @JsonProperty("storageAccount")
  @NotNull(message = "The account is required")
  @Valid
  private SecretKeySelector account;

  @JsonProperty("accessKey")
  @NotNull(message = "The accessKey is required")
  @Valid
  private SecretKeySelector accessKey;

  public SecretKeySelector getAccount() {
    return account;
  }

  public void setAccount(SecretKeySelector account) {
    this.account = account;
  }

  public SecretKeySelector getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(SecretKeySelector accessKey) {
    this.accessKey = accessKey;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
