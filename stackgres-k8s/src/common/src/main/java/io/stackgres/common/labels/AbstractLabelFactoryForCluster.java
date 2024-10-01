/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;

public abstract class AbstractLabelFactoryForCluster<T extends CustomResource<?, ?>>
    extends AbstractLabelFactory<T> implements LabelFactoryForCluster<T> {

  @Override
  public Map<String, String> clusterLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabelsWithoutUid(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .build();
  }

  @Override
  public Map<String, String> clusterLabelsWithoutUid(T resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceScopeKey(resource), labelValue(resourceScope(resource)))
        .put(labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> patroniClusterLabels(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceScopeKey(resource), labelValue(resourceScope(resource)),
        labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> clusterPrimaryLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabels(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.PRIMARY_ROLE)
        .build();
  }

  @Override
  public Map<String, String> clusterLabelsWithoutUidAndScope(T resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> clusterPrimaryLabelsWithoutUidAndScope(T resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabelsWithoutUidAndScope(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.PRIMARY_ROLE)
        .build();
  }

  @Override
  public Map<String, String> clusterReplicaLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabels(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.REPLICA_ROLE)
        .put(PatroniUtil.NOLOADBALANCE_TAG, PatroniUtil.FALSE_TAG_VALUE)
        .build();
  }

  @Override
  public Map<String, String> statefulSetPodLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabels(resource))
        .put(labelMapper().disruptableKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().scheduledBackupKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> clusterCrossNamespaceLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceNamespaceKey(resource), labelValue(resourceNamespace(resource)))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .build();
  }

  @Override
  public Map<String, String> replicationInitializationBackupLabels(T resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceScopeKey(resource), labelValue(resourceScope(resource)))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().replicationInitializationBackupKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

}
