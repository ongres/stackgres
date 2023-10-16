/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardeddbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsReshardingCitus;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedDbOpsResharding {

  private StackGresShardedDbOpsReshardingCitus citus;

  public StackGresShardedDbOpsReshardingCitus getCitus() {
    return citus;
  }

  public void setCitus(StackGresShardedDbOpsReshardingCitus citus) {
    this.citus = citus;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
