/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.common.resource.ResourceUtil.labelValue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;

public abstract class AbstractLabelFactoryForCluster<T extends CustomResource<?, ?>>
    extends AbstractLabelFactory<T> implements LabelFactoryForCluster<T> {

  @Override
  public Map<String, String> clusterLabels(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().resourceScopeKey(resource), labelValue(resourceScope(resource)),
        labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE,
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)));
  }

  @Override
  public Map<String, String> patroniClusterLabels(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceScopeKey(resource), labelValue(resourceScope(resource)),
        labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE);
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
  public Map<String, String> patroniPrimaryLabelsWithoutScope(T resource) {
    return ImmutableMap.<String, String>builder().putAll(patroniClusterLabelsWithoutScope(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.PRIMARY_ROLE)
        .build();
  }

  @Override
  public Map<String, String> patroniClusterLabelsWithoutScope(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)),
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> patroniReplicaLabelsWithoutScope(T resource) {
    return ImmutableMap.<String, String>builder().putAll(patroniClusterLabelsWithoutScope(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.REPLICA_ROLE)
        .build();
  }

  @Override
  public Map<String, String> statefulSetPodLabels(T resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE,
        labelMapper().disruptibleKey(resource), StackGresContext.RIGHT_VALUE,
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().resourceScopeKey(resource), labelValue(resourceScope(resource)),
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
        labelMapper().resourceNameKey(resource), labelValue(resourceName(resource)),
        labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)));
  }

}
