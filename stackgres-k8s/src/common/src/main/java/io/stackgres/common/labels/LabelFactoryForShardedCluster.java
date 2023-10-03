/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;

public interface LabelFactoryForShardedCluster
    extends LabelFactory<StackGresShardedCluster> {

  Map<String, String> coordinatorLabels(StackGresShardedCluster resource);

  Map<String, String> shardsLabels(StackGresShardedCluster resource);

  Map<String, String> scheduledBackupPodLabels(StackGresShardedCluster resource);

  @Override
  LabelMapperForShardedCluster labelMapper();

}
