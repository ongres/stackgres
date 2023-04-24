/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels.v14;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.labels.AbstractLabelFactory;
import io.stackgres.common.labels.LabelFactoryForCluster;

public abstract class AbstractLabelFactoryForCluster<T extends CustomResource<?, ?>>
    extends AbstractLabelFactory<T> implements LabelFactoryForCluster<T> {

  @Override
  public Map<String, String> clusterLabelsWithoutUid(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> clusterLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabelsWithoutUid(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .build();
  }

  @Override
  public Map<String, String> patroniClusterLabels(T resource) {
    return clusterLabels(resource);
  }

  @Override
  public Map<String, String> clusterPrimaryLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(patroniClusterLabels(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.PRIMARY_ROLE)
        .build();
  }

  @Override
  public Map<String, String> clusterReplicaLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(patroniClusterLabels(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.REPLICA_ROLE)
        .put(PatroniUtil.NOLOADBALANCE_TAG, PatroniUtil.FALSE_TAG_VALUE)
        .build();
  }

  @Override
  public Map<String, String> clusterLabelsWithoutUidAndScope(T resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabelsWithoutUid(resource))
        .build();
  }

  @Override
  public Map<String, String> clusterPrimaryLabelsWithoutUidAndScope(T resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabelsWithoutUidAndScope(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.PRIMARY_ROLE)
        .build();
  }

  @Override
  public Map<String, String> statefulSetPodLabels(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE,
        labelMapper().disruptibleKey(resource), StackGresContext.RIGHT_VALUE,
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)));
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().scheduledBackupKey(resource), StackGresContext.RIGHT_VALUE);
  }

  public Map<String, String> clusterCrossNamespaceLabels(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceNamespaceKey(resource), labelValue(resourceNamespace(resource)),
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)));
  }

}
