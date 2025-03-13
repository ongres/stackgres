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
public class OpenPgpStorageEncryptionDto {

  private SecretKeySelector key;

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
