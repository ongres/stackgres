/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs.dto.backupconfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.distributedlogs.dto.ResourceDto;

@RegisterForReflection
public class BackupConfigDto extends ResourceDto {

  @NotNull(message = "The specification of backup config is required")
  @Valid
  private BackupConfigSpec spec;

  public BackupConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(BackupConfigSpec spec) {
    this.spec = spec;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("metadata", getMetadata())
        .add("spec", spec)
        .toString();
  }

}
