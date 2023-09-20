/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
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
  public Map<String, String> clusterPrimaryLabels(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.clusterPrimaryLabels(resource);
    }
    return super.clusterPrimaryLabels(resource);
  }

  @Override
  public Map<String, String> clusterReplicaLabels(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.clusterReplicaLabels(resource);
    }
    return super.clusterReplicaLabels(resource);
  }

  @Override
  public Map<String, String> clusterPrimaryLabelsWithoutUidAndScope(StackGresCluster resource) {
    if (useV14(resource)) {
      return clusterLabelFactoryV14.clusterPrimaryLabelsWithoutUidAndScope(resource);
    }
    return super.clusterPrimaryLabelsWithoutUidAndScope(resource);
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
    return Optional.of(resource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(patroni -> patroni.getInitialConfig())
        .map(patroniConfig -> patroniConfig.getScope())
        .orElse(resourceName(resource));
  }

  private boolean useV14(StackGresCluster resource) {
    return StackGresVersion.getStackGresVersion(resource)
        .compareTo(StackGresVersion.V_1_4) <= 0;
  }

}
