/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ClusterLabelFactory extends AbstractLabelFactoryForCluster<StackGresCluster> {

  private final ClusterLabelMapper labelMapper;

  @Inject
  public ClusterLabelFactory(ClusterLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public ClusterLabelMapper labelMapper() {
    return labelMapper;
  }

  @Override
  public Map<String, String> clusterLabels(StackGresCluster resource) {
    return super.clusterLabels(resource);
  }

  @Override
  public Map<String, String> patroniClusterLabels(StackGresCluster resource) {
    return super.patroniClusterLabels(resource);
  }

  @Override
  public Map<String, String> clusterPrimaryLabels(StackGresCluster resource) {
    return super.clusterPrimaryLabels(resource);
  }

  @Override
  public Map<String, String> clusterReplicaLabels(StackGresCluster resource) {
    return super.clusterReplicaLabels(resource);
  }

  @Override
  public Map<String, String> clusterPrimaryLabelsWithoutUidAndScope(StackGresCluster resource) {
    return super.clusterPrimaryLabelsWithoutUidAndScope(resource);
  }

  @Override
  public Map<String, String> statefulSetPodLabels(StackGresCluster resource) {
    return super.statefulSetPodLabels(resource);
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(StackGresCluster resource) {
    return super.scheduledBackupPodLabels(resource);
  }

  @Override
  public Map<String, String> clusterCrossNamespaceLabels(StackGresCluster resource) {
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

}
