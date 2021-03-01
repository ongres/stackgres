/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;

public interface LabelFactory<T extends CustomResource<?, ?>> {

  Map<String, String> genericClusterLabels(StackGresCluster resource);

  Map<String, String> clusterLabels(StackGresCluster resource);

  Map<String, String> patroniClusterLabels(StackGresCluster resource);

  Map<String, String> patroniPrimaryLabels(StackGresCluster resource);

  Map<String, String> patroniReplicaLabels(StackGresCluster resource);

  Map<String, String> statefulSetPodLabels(StackGresCluster resource);

  Map<String, String> backupPodLabels(StackGresCluster resource);

  Map<String, String> scheduledBackupPodLabels(StackGresCluster resource);

  Map<String, String> dbOpsPodLabels(StackGresCluster resource);

  Map<String, String> anyPatroniClusterLabels();

  Map<String, String> clusterCrossNamespaceLabels(StackGresCluster resource);

  LabelMapper<T> getLabelMapper();

  default String clusterName(StackGresCluster resource) {
    return resource.getMetadata().getName();
  }

  default String clusterNamespace(StackGresCluster resource) {
    return resource.getMetadata().getNamespace();
  }

  default String clusterUid(StackGresCluster resource) {
    return resource.getMetadata().getUid();
  }

  default String clusterScope(StackGresCluster resource) {
    return ResourceUtil.labelValue(clusterName(resource));
  }

  default List<OwnerReference> ownerReferences(StackGresCluster resource) {
    return ImmutableList.of(ResourceUtil.getOwnerReference(resource));
  }

}
