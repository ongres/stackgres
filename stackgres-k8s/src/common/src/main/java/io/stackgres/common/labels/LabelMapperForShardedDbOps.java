/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;

public interface LabelMapperForShardedDbOps
    extends LabelMapper<StackGresShardedDbOps> {

  default String dbOpsKey(StackGresShardedDbOps resource) {
    return getKeyPrefix(resource) + StackGresContext.SHARDED_DBOPS_KEY;
  }

}
