/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;

public interface LabelFactoryForShardedDbOps
    extends LabelFactory<StackGresShardedDbOps> {

  Map<String, String> shardedDbOpsPodLabels(StackGresShardedDbOps resource);

  Map<String, String> shardedDbOpsLabels(StackGresShardedDbOps resource);

  @Override
  LabelMapperForShardedDbOps labelMapper();

}
