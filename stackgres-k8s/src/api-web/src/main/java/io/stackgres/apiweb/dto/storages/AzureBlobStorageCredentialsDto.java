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

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AzureBlobStorageCredentialsDto {

  @JsonProperty("storageAccount")
  private String account;

  @JsonProperty("accessKey")
  private String accessKey;

  @JsonProperty("secretKeySelectors")
  private AzureBlobSecretKeySelectorDto secretKeySelectors;

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

  public AzureBlobSecretKeySelectorDto getSecretKeySelectors() {
    return secretKeySelectors;
  }

  public void setSecretKeySelectors(AzureBlobSecretKeySelectorDto secretKeySelectors) {
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
    AzureBlobStorageCredentialsDto that = (AzureBlobStorageCredentialsDto) o;
    return Objects.equals(account, that.account)
        && Objects.equals(accessKey, that.accessKey)
        && Objects.equals(secretKeySelectors, that.secretKeySelectors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(account, accessKey, secretKeySelectors);
  }
}
