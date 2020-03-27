/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ConfigProperty;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.customresource.sgcluster.StackgresClusterConfiguration;
import io.stackgres.operator.rest.dto.cluster.ClusterConfiguration;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterRestore;
import io.stackgres.operator.rest.dto.cluster.ClusterSpec;
import io.stackgres.operator.rest.dto.cluster.NonProduction;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterTransformer
    extends AbstractResourceTransformer<ClusterDto, StackGresCluster> {

  private ConfigContext context;
  private ClusterPodTransformer clusterPodTransformer;

  @Override
  public StackGresCluster toCustomResource(ClusterDto source) {
    StackGresCluster transformation = new StackGresCluster();
    transformation.setMetadata(getCustomResourceMetadata(source));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public ClusterDto toResource(StackGresCluster source) {
    ClusterDto transformation = new ClusterDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setGrafanaEmbedded(isGrafanaEmbeddedEnabled());
    return transformation;
  }

  public ClusterDto toResourceWithPods(StackGresCluster source, List<Pod> pods) {
    ClusterDto clusterDto = toResource(source);

    clusterDto.setPods(Seq.seq(pods)
        .map(clusterPodTransformer::toResource)
        .toList());

    clusterDto.setPodsReady((int) clusterDto.getPods()
        .stream()
        .filter(pod -> pod.getContainers().equals(pod.getContainersReady()))
        .count());

    return clusterDto;
  }

  private boolean isGrafanaEmbeddedEnabled() {
    return context.getProperty(ConfigProperty.GRAFANA_EMBEDDED)
        .map(Boolean::parseBoolean)
        .orElse(false);
  }

  public StackGresClusterSpec getCustomResourceSpec(ClusterSpec source) {
    StackGresClusterSpec transformation = new StackGresClusterSpec();
    transformation.setConfigurations(new StackgresClusterConfiguration());
    transformation.getConfigurations().setBackupConfig(
        source.getConfigurations().getBackupConfig());
    transformation.getConfigurations()
        .setConnectionPoolingConfig(source.getConfigurations().getConnectionPoolingConfig());
    transformation.setInstances(source.getInstances());
    transformation.setNonProduction(
        getCustomResourceNonProduction(source.getNonProduction()));
    transformation.getConfigurations().setPostgresConfig(
        source.getConfigurations().getPostgresConfig());
    transformation.setPostgresVersion(source.getPostgresVersion());
    transformation.setPrometheusAutobind(source.getPrometheusAutobind());
    transformation.setResourceProfile(source.getResourceProfile());
    transformation.setRestore(
        getCustomResourceRestore(source.getRestore()));
    transformation.setSidecars(source.getSidecars());
    transformation.setStorageClass(source.getStorageClass());
    transformation.setVolumeSize(source.getVolumeSize());
    return transformation;
  }

  private io.stackgres.operator.customresource.sgcluster.NonProduction
      getCustomResourceNonProduction(NonProduction source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.sgcluster.NonProduction transformation =
        new io.stackgres.operator.customresource.sgcluster.NonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private io.stackgres.operator.customresource.sgcluster.ClusterRestore getCustomResourceRestore(
      ClusterRestore source) {
    if (source == null) {
      return null;
    }
    io.stackgres.operator.customresource.sgcluster.ClusterRestore transformation =
        new io.stackgres.operator.customresource.sgcluster.ClusterRestore();
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setBackupUid(source.getBackupUid());
    return transformation;
  }

  public ClusterSpec getResourceSpec(StackGresClusterSpec source) {
    ClusterSpec transformation = new ClusterSpec();
    transformation.setConfigurations(new ClusterConfiguration());
    transformation.getConfigurations().setBackupConfig(
        source.getConfigurations().getBackupConfig());
    transformation.getConfigurations().setConnectionPoolingConfig(source
        .getConfigurations().getConnectionPoolingConfig());
    transformation.setInstances(source.getInstances());
    transformation.setNonProduction(
        getResourceNonProduction(source.getNonProduction()));
    transformation.getConfigurations().setPostgresConfig(
        source.getConfigurations().getPostgresConfig());
    transformation.setPostgresVersion(source.getPostgresVersion());
    transformation.setPrometheusAutobind(source.getPrometheusAutobind());
    transformation.setResourceProfile(source.getResourceProfile());
    transformation.setRestore(
        getResourceRestore(source.getRestore()));
    transformation.setSidecars(source.getSidecars());
    transformation.setStorageClass(source.getStorageClass());
    transformation.setVolumeSize(source.getVolumeSize());
    return transformation;
  }

  private NonProduction getResourceNonProduction(
      io.stackgres.operator.customresource.sgcluster.NonProduction source) {
    if (source == null) {
      return null;
    }
    NonProduction transformation = new NonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private ClusterRestore getResourceRestore(
      io.stackgres.operator.customresource.sgcluster.ClusterRestore source) {
    if (source == null) {
      return null;
    }
    ClusterRestore transformation = new ClusterRestore();
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setBackupUid(source.getBackupUid());
    return transformation;
  }

  @Inject
  public void setContext(ConfigContext context) {
    this.context = context;
  }

  @Inject
  public void setClusterPodTransformer(ClusterPodTransformer clusterPodTransformer) {
    this.clusterPodTransformer = clusterPodTransformer;
  }
}
