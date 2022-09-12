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
public class AzureBlobSecretKeySelectorDto {

  @JsonProperty("storageAccount")
  private SecretKeySelector account;

  @JsonProperty("accessKey")
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AzureBlobSecretKeySelectorDto that = (AzureBlobSecretKeySelectorDto) o;
    return Objects.equals(account, that.account)
        && Objects.equals(accessKey, that.accessKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(account, accessKey);
  }
}
