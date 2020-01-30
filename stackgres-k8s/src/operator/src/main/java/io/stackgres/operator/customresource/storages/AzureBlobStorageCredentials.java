/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.storages;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class AzureBlobStorageCredentials {

  @JsonProperty("account")
  @NotNull(message = "The account is required")
  private SecretKeySelector account;

  @JsonProperty("accessKey")
  @NotNull(message = "The accessKey is required")
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
  public int hashCode() {
    return Objects.hash(accessKey, account);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AzureBlobStorageCredentials)) {
      return false;
    }
    AzureBlobStorageCredentials other = (AzureBlobStorageCredentials) obj;
    return Objects.equals(accessKey, other.accessKey) && Objects.equals(account, other.account);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("account", account)
        .add("accessKey", accessKey)
        .toString();
  }

}
