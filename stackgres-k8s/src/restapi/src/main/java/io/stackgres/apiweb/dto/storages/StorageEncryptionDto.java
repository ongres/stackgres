/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StorageEncryptionDto {

  private String method;

  private SodiumStorageEncryptionDto sodium;

  private OpenPgpStorageEncryptionDto openpgp;

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public SodiumStorageEncryptionDto getSodium() {
    return sodium;
  }

  public void setSodium(SodiumStorageEncryptionDto sodium) {
    this.sodium = sodium;
  }

  public OpenPgpStorageEncryptionDto getOpenpgp() {
    return openpgp;
  }

  public void setOpenpgp(OpenPgpStorageEncryptionDto openpgp) {
    this.openpgp = openpgp;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
