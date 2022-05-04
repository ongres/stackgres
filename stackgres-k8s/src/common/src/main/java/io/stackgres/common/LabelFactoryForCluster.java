/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import io.fabric8.kubernetes.client.CustomResource;

public interface LabelFactoryForCluster<T extends CustomResource<?, ?>>
    extends LabelFactory<T> {

  Map<String, String> clusterLabels(T resource);

  Map<String, String> patroniClusterLabels(T resource);

  Map<String, String> patroniPrimaryLabels(T resource);

  Map<String, String> patroniReplicaLabels(T resource);

  Map<String, String> statefulSetPodLabels(T resource);

  Map<String, String> scheduledBackupPodLabels(T resource);

  Map<String, String> anyPatroniClusterLabels();

  Map<String, String> clusterCrossNamespaceLabels(T resource);

  @Override
  LabelMapperForCluster labelMapper();

}
