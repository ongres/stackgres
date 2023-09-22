/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedbackup;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@ResourceClassForDto(StackGresShardedBackup.class)
public class ShardedBackupDto extends ResourceDto {

  private ShardedBackupSpec spec;

  private ShardedBackupStatus status;

  public ShardedBackupSpec getSpec() {
    return spec;
  }

  public void setSpec(ShardedBackupSpec spec) {
    this.spec = spec;
  }

  public ShardedBackupStatus getStatus() {
    return status;
  }

  public void setStatus(ShardedBackupStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
