/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backupconfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BackupConfigSpec {

  @JsonProperty("storage")
  @NotNull(message = "The storage is required")
  @Valid
  private BackupStorageDto storage;

  @JsonProperty("baseBackups")
  @NotNull(message = "Base backup configuration is required")
  @Valid
  private BaseBackupConfig baseBackups;

  public BackupStorageDto getStorage() {
    return storage;
  }

  public void setStorage(BackupStorageDto storage) {
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
    return StackGresUtil.toPrettyYaml(this);
  }

}
