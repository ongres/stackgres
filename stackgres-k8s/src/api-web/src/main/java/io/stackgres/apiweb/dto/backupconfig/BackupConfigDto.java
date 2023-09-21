/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backupconfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ResourceClassForDto(StackGresBackupConfig.class)
public class BackupConfigDto extends ResourceDto {

  private BackupConfigSpec spec;

  private BackupConfigStatus status;

  public BackupConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(BackupConfigSpec spec) {
    this.spec = spec;
  }

  public BackupConfigStatus getStatus() {
    return status;
  }

  public void setStatus(BackupConfigStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
