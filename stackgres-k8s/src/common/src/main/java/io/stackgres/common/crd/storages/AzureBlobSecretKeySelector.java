/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class AzureBlobSecretKeySelector {

  @JsonProperty("storageAccount")
  @NotNull(message = "The account is required")
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
  public int hashCode() {
    return Objects.hash(account, accessKey);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AzureBlobSecretKeySelector that = (AzureBlobSecretKeySelector) o;
    return Objects.equals(account, that.account)
        && Objects.equals(accessKey, that.accessKey);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
