/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardeddbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@ResourceClassForDto(StackGresShardedDbOps.class)
public final class ShardedDbOpsDto extends ResourceDto {

  private ShardedDbOpsSpec spec;

  private ShardedDbOpsStatus status;

  public ShardedDbOpsSpec getSpec() {
    return spec;
  }

  public void setSpec(ShardedDbOpsSpec spec) {
    this.spec = spec;
  }

  public ShardedDbOpsStatus getStatus() {
    return status;
  }

  public void setStatus(ShardedDbOpsStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
