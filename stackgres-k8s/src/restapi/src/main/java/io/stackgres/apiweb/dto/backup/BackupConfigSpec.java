/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backup;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BackupConfigSpec {

  private BackupStorageDto storage;

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

  public void setBaseBackups(BaseBackupConfig baseBackups) {
    this.baseBackups = baseBackups;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
