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
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class SodiumStorageEncryption {

  @NotNull(message = "The key is required")
  @Valid
  private SecretKeySelector key;

  @ValidEnum(enumClass = SodiumKeyTransformation.class, allowNulls = true,
      message = "keyTransform can be base64, hex or none")
  private String keyTransform;

  public SecretKeySelector getKey() {
    return key;
  }

  public void setKey(SecretKeySelector key) {
    this.key = key;
  }

  public String getKeyTransform() {
    return keyTransform;
  }

  public void setKeyTransform(String keyTransform) {
    this.keyTransform = keyTransform;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, keyTransform);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SodiumStorageEncryption)) {
      return false;
    }
    SodiumStorageEncryption other = (SodiumStorageEncryption) obj;
    return Objects.equals(key, other.key) && Objects.equals(keyTransform, other.keyTransform);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
