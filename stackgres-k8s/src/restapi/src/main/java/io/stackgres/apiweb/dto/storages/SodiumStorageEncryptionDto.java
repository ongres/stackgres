/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class SodiumStorageEncryptionDto {

  private SecretKeySelector key;

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
