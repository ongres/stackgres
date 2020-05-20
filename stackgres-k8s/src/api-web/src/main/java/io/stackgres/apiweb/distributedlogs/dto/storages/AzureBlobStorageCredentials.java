/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs.dto.storages;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class AzureBlobStorageCredentials {

  @JsonProperty("storageAccount")
  private String account;

  @JsonProperty("accessKey")
  private String accessKey;

  @JsonProperty("secretKeySelectors")
  @NotNull(message = "The secret Key Selectors are required")
  @Valid
  private AzureBlobSecretKeySelector secretKeySelectors;

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public AzureBlobSecretKeySelector getSecretKeySelectors() {
    return secretKeySelectors;
  }

  public void setSecretKeySelectors(AzureBlobSecretKeySelector secretKeySelectors) {
    this.secretKeySelectors = secretKeySelectors;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("account", account)
        .add("accessKey", accessKey)
        .add("secretKeySelectors", secretKeySelectors)
        .toString();
  }

}
