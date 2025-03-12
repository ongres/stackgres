/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class OpenPgpStorageEncryption {

  @NotNull(message = "The key is required")
  @Valid
  private SecretKeySelector key;

  @Valid
  private SecretKeySelector keyPassphrase;

  public SecretKeySelector getKey() {
    return key;
  }

  public void setKey(SecretKeySelector key) {
    this.key = key;
  }

  public SecretKeySelector getKeyPassphrase() {
    return keyPassphrase;
  }

  public void setKeyPassphrase(SecretKeySelector keyPassphrase) {
    this.keyPassphrase = keyPassphrase;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, keyPassphrase);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof OpenPgpStorageEncryption)) {
      return false;
    }
    OpenPgpStorageEncryption other = (OpenPgpStorageEncryption) obj;
    return Objects.equals(key, other.key) && Objects.equals(keyPassphrase, other.keyPassphrase);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
