/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs.dto.backup;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.distributedlogs.dto.ResourceDto;

@RegisterForReflection
public class BackupDto extends ResourceDto {

  @NotNull(message = "The specification of backup is required")
  @Valid
  private BackupSpec spec;

  @Valid
  private BackupStatus status;

  public BackupSpec getSpec() {
    return spec;
  }

  public void setSpec(BackupSpec spec) {
    this.spec = spec;
  }

  public BackupStatus getStatus() {
    return status;
  }

  public void setStatus(BackupStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("metadata", getMetadata())
        .add("spec", spec)
        .add("status", status)
        .toString();
  }

}
