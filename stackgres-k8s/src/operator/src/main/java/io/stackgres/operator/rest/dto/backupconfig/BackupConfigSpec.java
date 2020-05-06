/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.backupconfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.rest.dto.storages.BackupStorage;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BackupConfigSpec {

  @JsonProperty("storage")
  @NotNull(message = "The storage is required")
  private BackupStorage storage;

  @JsonProperty("baseBackups")
  @NotNull(message = "Base backup configuration is required")
  @Valid
  private BaseBackupConfig baseBackups;

  public BackupStorage getStorage() {
    return storage;
  }

  public void setStorage(BackupStorage storage) {
    this.storage = storage;
  }

  public BaseBackupConfig getBaseBackups() {
    return baseBackups;
  }

  public void setBaseBackup(BaseBackupConfig baseBackups) {
    this.baseBackups = baseBackups;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("storage", storage)
        .add("baseBackups", baseBackups)
        .toString();
  }

}
