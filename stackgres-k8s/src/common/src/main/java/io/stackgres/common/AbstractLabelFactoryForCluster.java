/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResource;

public abstract class AbstractLabelFactoryForCluster<T extends CustomResource<?, ?>>
    extends AbstractLabelFactory<T> implements LabelFactoryForCluster<T> {

  @Override
  public Map<String, String> clusterLabels(T resource) {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceNameKey(), labelValue(resourceName(resource)),
        labelMapper().resourceUidKey(), labelValue(resourceUid(resource)));
  }

  @Override
  public Map<String, String> patroniClusterLabels(T resource) {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(), labelValue(resourceName(resource)),
        labelMapper().clusterKey(), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> patroniPrimaryLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(patroniClusterLabels(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.PRIMARY_ROLE)
        .build();
  }

  @Override
  public Map<String, String> patroniReplicaLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(patroniClusterLabels(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.REPLICA_ROLE)
        .build();
  }

  @Override
  public Map<String, String> statefulSetPodLabels(T resource) {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().clusterKey(), StackGresContext.RIGHT_VALUE,
        labelMapper().disruptibleKey(), StackGresContext.RIGHT_VALUE,
        labelMapper().resourceNameKey(), labelValue(resourceName(resource)),
        labelMapper().resourceUidKey(), labelValue(resourceUid(resource)));
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(T resource) {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(), labelValue(resourceName(resource)),
        labelMapper().scheduledBackupKey(), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> anyPatroniClusterLabels() {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().clusterKey(), StackGresContext.RIGHT_VALUE);
  }

  public Map<String, String> clusterCrossNamespaceLabels(T resource) {
    return ImmutableMap.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceNamespaceKey(), labelValue(resourceNamespace(resource)),
        labelMapper().resourceUidKey(), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(), labelValue(resourceName(resource)));
  }

}
