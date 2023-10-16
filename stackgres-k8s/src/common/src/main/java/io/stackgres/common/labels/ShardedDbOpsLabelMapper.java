/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;

@ApplicationScoped
public class ShardedDbOpsLabelMapper implements LabelMapperForShardedDbOps {

  @Override
  public String appName() {
    return StackGresContext.SHARDED_DBOPS_APP_NAME;
  }

  @Override
  public String resourceNameKey(StackGresShardedDbOps resource) {
    return getKeyPrefix(resource) + StackGresContext.SHARDED_DBOPS_NAME_KEY;
  }

  @Override
  public String resourceNamespaceKey(StackGresShardedDbOps resource) {
    return getKeyPrefix(resource) + StackGresContext.SHARDED_DBOPS_NAMESPACE_KEY;
  }

  @Override
  public String resourceUidKey(StackGresShardedDbOps resource) {
    return getKeyPrefix(resource) + StackGresContext.SHARDED_DBOPS_UID_KEY;
  }

}
