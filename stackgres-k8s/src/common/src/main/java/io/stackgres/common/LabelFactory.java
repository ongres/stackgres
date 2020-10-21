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

  Map<String, String> genericClusterLabels(T resource);

  Map<String, String> clusterLabels(T resource);

  Map<String, String> patroniClusterLabels(T resource);

  Map<String, String> patroniPrimaryLabels(T resource);

  Map<String, String> patroniReplicaLabels(T resource);

  Map<String, String> statefulSetPodLabels(T resource);

  Map<String, String> backupPodLabels(T resource);

  Map<String, String> scheduledBackupPodLabels(T resource);

  Map<String, String> dbOpsPodLabels(T resource);

  Map<String, String> anyPatroniClusterLabels();

  Map<String, String> clusterCrossNamespaceLabels(T resource);

  LabelMapper<T> getLabelMapper();

  default String clusterName(T resource) {
    return resource.getMetadata().getName();
  }

  default String clusterNamespace(T resource) {
    return resource.getMetadata().getNamespace();
  }

  default String clusterUid(T resource) {
    return resource.getMetadata().getUid();
  }

  default String clusterScope(T resource) {
    return ResourceUtil.labelValue(clusterName(resource));
  }

  default List<OwnerReference> ownerReferences(StackGresCluster resource) {
    return ImmutableList.of(ResourceUtil.getOwnerReference(resource));
  }

}
