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
public class AzureBlobStorageCredentials {

  @JsonProperty("account")
  private String account;

  @JsonProperty("accessKey")
  private String accessKey;

  @JsonProperty("accountSelector")
  private SecretKeySelector accountSelector;

  @JsonProperty("accessKeySelector")
  private SecretKeySelector accessKeySelector;

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

  public SecretKeySelector getAccountSelector() {
    return accountSelector;
  }

  public void setAccountSelector(SecretKeySelector accountSelector) {
    this.accountSelector = accountSelector;
  }

  public SecretKeySelector getAccessKeySelector() {
    return accessKeySelector;
  }

  public void setAccessKeySelector(SecretKeySelector accessKeySelector) {
    this.accessKeySelector = accessKeySelector;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("account", account)
        .add("accessKey", accessKey)
        .add("accountSelector", accountSelector)
        .add("accessKeySelector", accessKeySelector)
        .toString();
  }

}
