/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.v14.ClusterLabelFactoryV14;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ClusterLabelFactory extends AbstractLabelFactoryForCluster<StackGresCluster> {

  private final ClusterLabelMapper labelMapper;

  private final ClusterLabelFactoryV14 clusterLabelFactoryV14;

  @Inject
  public ClusterLabelFactory(ClusterLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
    this.clusterLabelFactoryV14 = new ClusterLabelFactoryV14(labelMapper);
  }

  @Override
  public ClusterLabelMapper labelMapper() {
    return labelMapper;
  }

  @Override
  public Map<String, String> clusterLabels(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.clusterLabels(resource);
    }
    return super.clusterLabels(resource);
  }

  @Override
  public Map<String, String> patroniClusterLabels(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.patroniClusterLabels(resource);
    }
    return super.patroniClusterLabels(resource);
  }

  @Override
  public Map<String, String> patroniPrimaryLabels(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.patroniPrimaryLabels(resource);
    }
    return super.patroniPrimaryLabels(resource);
  }

  @Override
  public Map<String, String> patroniPrimaryLabelsWithoutScope(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.patroniPrimaryLabelsWithoutScope(resource);
    }
    return super.patroniPrimaryLabelsWithoutScope(resource);
  }

  @Override
  public Map<String, String> patroniClusterLabelsWithoutScope(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.patroniClusterLabelsWithoutScope(resource);
    }
    return super.patroniClusterLabelsWithoutScope(resource);
  }

  @Override
  public Map<String, String> patroniReplicaLabelsWithoutScope(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.patroniReplicaLabelsWithoutScope(resource);
    }
    return super.patroniReplicaLabelsWithoutScope(resource);
  }

  @Override
  public Map<String, String> statefulSetPodLabels(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.statefulSetPodLabels(resource);
    }
    return super.statefulSetPodLabels(resource);
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.scheduledBackupPodLabels(resource);
    }
    return super.scheduledBackupPodLabels(resource);
  }

  @Override
  public Map<String, String> clusterCrossNamespaceLabels(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.clusterCrossNamespaceLabels(resource);
    }
    return super.clusterCrossNamespaceLabels(resource);
  }

  @Override
  public String resourceScope(@NotNull StackGresCluster resource) {
    return Optional.ofNullable(resource.getSpec().getConfiguration().getPatroni())
        .map(patroni -> patroni.getInitialConfig())
        .map(patroniConfig -> patroniConfig.getScope())
        .orElse(resourceName(resource));
  }

  @Override
  public Map<String, String> patroniReplicaLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(super.patroniReplicaLabels(resource))
        .put(PatroniUtil.NOLOADBALANCE_TAG, PatroniUtil.FALSE_TAG_VALUE)
        .build();
  }

  private boolean useV14(StackGresCluster resource) {
    return StackGresVersion.getStackGresVersion(resource)
        .compareTo(StackGresVersion.V_1_4) <= 0;
  }

}
