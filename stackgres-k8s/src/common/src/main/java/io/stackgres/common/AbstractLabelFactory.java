/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.resource.ResourceUtil;

public abstract class AbstractLabelFactory<T extends CustomResource<?, ?>>
    implements LabelFactory<T> {
  @Override
  public Map<String, String> genericClusterLabels(T resource) {
    return ImmutableMap.of(getLabelMapper().appKey(), getLabelMapper().appName(),
        getLabelMapper().clusterNameKey(), ResourceUtil.labelValue(clusterName(resource)));
  }

  @Override
  public Map<String, String> clusterLabels(T resource) {
    return ImmutableMap.of(getLabelMapper().appKey(), getLabelMapper().appName(),
        getLabelMapper().clusterUidKey(), ResourceUtil.labelValue(clusterUid(resource)),
        getLabelMapper().clusterNameKey(), ResourceUtil.labelValue(clusterName(resource)));
  }

  @Override
  public Map<String, String> patroniClusterLabels(T resource) {
    return ImmutableMap.of(getLabelMapper().appKey(), getLabelMapper().appName(),
        getLabelMapper().clusterUidKey(), ResourceUtil.labelValue(clusterUid(resource)),
        getLabelMapper().clusterNameKey(), ResourceUtil.labelValue(clusterName(resource)),
        getLabelMapper().clusterKey(), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> patroniPrimaryLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(patroniClusterLabels(resource))
        .put(StackGresContext.ROLE_KEY, StackGresContext.PRIMARY_ROLE)
        .build();
  }

  @Override
  public Map<String, String> patroniReplicaLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(patroniClusterLabels(resource))
        .put(StackGresContext.ROLE_KEY, StackGresContext.REPLICA_ROLE)
        .build();
  }

  @Override
  public Map<String, String> statefulSetPodLabels(T resource) {
    return ImmutableMap.of(getLabelMapper().appKey(), getLabelMapper().appName(),
        getLabelMapper().clusterUidKey(), ResourceUtil.labelValue(clusterUid(resource)),
        getLabelMapper().clusterNameKey(), ResourceUtil.labelValue(clusterName(resource)),
        getLabelMapper().clusterKey(), StackGresContext.RIGHT_VALUE,
        getLabelMapper().disruptibleKey(), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> backupPodLabels(T resource) {
    return ImmutableMap.of(getLabelMapper().appKey(), getLabelMapper().appName(),
        getLabelMapper().clusterUidKey(), ResourceUtil.labelValue(clusterUid(resource)),
        getLabelMapper().clusterNameKey(), ResourceUtil.labelValue(clusterName(resource)),
        getLabelMapper().backupKey(), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(T resource) {
    return ImmutableMap.of(getLabelMapper().appKey(), getLabelMapper().appName(),
        getLabelMapper().clusterUidKey(), ResourceUtil.labelValue(clusterUid(resource)),
        getLabelMapper().clusterNameKey(), ResourceUtil.labelValue(clusterName(resource)),
        getLabelMapper().scheduledBackupKey(), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> dbOpsPodLabels(T resource) {
    return ImmutableMap.of(getLabelMapper().appKey(), getLabelMapper().appName(),
        getLabelMapper().clusterUidKey(), ResourceUtil.labelValue(clusterUid(resource)),
        getLabelMapper().clusterNameKey(), ResourceUtil.labelValue(clusterName(resource)),
        getLabelMapper().dbOpsKey(), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> anyPatroniClusterLabels() {
    return ImmutableMap.of(getLabelMapper().appKey(), getLabelMapper().appName(),
        getLabelMapper().clusterKey(), StackGresContext.RIGHT_VALUE);
  }

  public Map<String, String> clusterCrossNamespaceLabels(T resource) {
    return ImmutableMap.of(getLabelMapper().appKey(), getLabelMapper().appName(),
        getLabelMapper().clusterNamespaceKey(), ResourceUtil.labelValue(clusterNamespace(resource)),
        getLabelMapper().clusterUidKey(), ResourceUtil.labelValue(clusterUid(resource)),
        getLabelMapper().clusterNameKey(), ResourceUtil.labelValue(clusterName(resource)));
  }

}
