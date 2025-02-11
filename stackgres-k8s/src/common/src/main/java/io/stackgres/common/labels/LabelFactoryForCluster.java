/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import org.jetbrains.annotations.NotNull;

public interface LabelFactoryForCluster
    extends LabelFactory<StackGresCluster> {

  Map<String, String> defaultConfigLabels(StackGresCluster resource);

  Map<String, String> clusterLabels(StackGresCluster resource);

  Map<String, String> clusterLabelsWithoutUid(StackGresCluster resource);

  Map<String, String> patroniClusterLabels(StackGresCluster resource);

  Map<String, String> clusterPrimaryLabels(StackGresCluster resource);

  Map<String, String> clusterReplicaLabels(StackGresCluster resource);

  Map<String, String> clusterLabelsWithoutUidAndScope(StackGresCluster resource);

  Map<String, String> clusterPrimaryLabelsWithoutUidAndScope(StackGresCluster resource);

  Map<String, String> statefulSetPodLabels(StackGresCluster resource);

  Map<String, String> scheduledBackupPodLabels(StackGresCluster resource);

  Map<String, String> clusterCrossNamespaceLabels(StackGresCluster resource);

  Map<String, String> replicationInitializationBackupLabels(StackGresCluster resource);

  String resourceScope(@NotNull StackGresCluster resource);

  @Override
  LabelMapperForCluster labelMapper();

}
