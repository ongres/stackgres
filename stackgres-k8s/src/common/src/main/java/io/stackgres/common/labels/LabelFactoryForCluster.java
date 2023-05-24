/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.fabric8.kubernetes.client.CustomResource;
import org.jetbrains.annotations.NotNull;

public interface LabelFactoryForCluster<T extends CustomResource<?, ?>>
    extends LabelFactory<T> {

  Map<String, String> clusterLabels(T resource);

  Map<String, String> clusterLabelsWithoutUid(T resource);

  Map<String, String> patroniClusterLabels(T resource);

  Map<String, String> patroniPrimaryLabels(T resource);

  Map<String, String> patroniReplicaLabels(T resource);

  Map<String, String> patroniClusterLabelsWithoutScope(T resource);

  Map<String, String> patroniPrimaryLabelsWithoutScope(T resource);

  Map<String, String> patroniReplicaLabelsWithoutScope(T resource);

  Map<String, String> statefulSetPodLabels(T resource);

  Map<String, String> scheduledBackupPodLabels(T resource);

  Map<String, String> clusterCrossNamespaceLabels(T resource);

  String resourceScope(@NotNull T resource);

  @Override
  LabelMapperForCluster<T> labelMapper();

}
