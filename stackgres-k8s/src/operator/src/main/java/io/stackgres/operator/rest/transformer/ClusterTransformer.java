/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackgresClusterConfiguration;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ConfigProperty;
import io.stackgres.operator.rest.dto.cluster.ClusterConfiguration;
import io.stackgres.operator.rest.dto.cluster.ClusterDistributedLogs;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterInitData;
import io.stackgres.operator.rest.dto.cluster.ClusterPod;
import io.stackgres.operator.rest.dto.cluster.ClusterPodMetadata;
import io.stackgres.operator.rest.dto.cluster.ClusterPodPersistentVolume;
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
  public StackGresCluster toCustomResource(ClusterDto source, StackGresCluster original) {
    StackGresCluster transformation = Optional.ofNullable(original)
        .orElseGet(StackGresCluster::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
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
    if (source == null) {
      return null;
    }
    StackGresClusterSpec transformation = new StackGresClusterSpec();
    transformation.setConfiguration(new StackgresClusterConfiguration());
    transformation.getConfiguration().setBackupConfig(
        source.getConfigurations().getSgBackupConfig());
    transformation.getConfiguration()
        .setConnectionPoolingConfig(source.getConfigurations().getSgPoolingConfig());
    transformation.setInstances(source.getInstances());
    transformation.setNonProduction(
        getCustomResourceNonProduction(source.getNonProduction()));
    transformation.getConfiguration().setPostgresConfig(
        source.getConfigurations().getSgPostgresConfig());
    transformation.setPostgresVersion(source.getPostgresVersion());
    transformation.setPrometheusAutobind(source.getPrometheusAutobind());
    transformation.setResourceProfile(source.getSgInstanceProfile());
    Optional.ofNullable(source.getInitData())
        .map(ClusterInitData::getRestore)
        .ifPresent(clusterRestore -> {
          transformation.setInitData(new StackGresClusterInitData());
          transformation.getInitData().setRestore(
              getCustomResourceRestore(source.getInitData().getRestore()));
        });

    final StackGresClusterPod targetPod = new StackGresClusterPod();
    transformation.setPod(targetPod);
    targetPod.setPersistentVolume(new StackGresPodPersistentVolume());
    targetPod.getPersistentVolume().setStorageClass(
        source.getPods().getPersistentVolume().getStorageClass());
    targetPod.getPersistentVolume().setVolumeSize(
        source.getPods().getPersistentVolume().getVolumeSize());

    targetPod
        .setDisableConnectionPooling(source.getPods().getDisableConnectionPooling());
    targetPod
        .setDisableMetricsExporter(source.getPods().getDisableMetricsExporter());
    targetPod
        .setDisablePostgresUtil(source.getPods().getDisablePostgresUtil());

    targetPod.setMetadata(Optional.ofNullable(source.getPods().getMetadata())
        .map(sourcePodMetadata -> {
          StackGresClusterPodMetadata targetMetadata = new StackGresClusterPodMetadata();
          targetMetadata.setAnnotations(sourcePodMetadata.getAnnotations());
          targetMetadata.setLabels(sourcePodMetadata.getLabels());
          return targetMetadata;
        }).orElse(null));

    transformation.setDistributedLogs(
        getCustomResourceDistributedLogs(source.getDistributedLogs()));

    return transformation;
  }

  private io.stackgres.common.crd.sgcluster.NonProduction
      getCustomResourceNonProduction(NonProduction source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.sgcluster.NonProduction transformation =
        new io.stackgres.common.crd.sgcluster.NonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private io.stackgres.common.crd.sgcluster.ClusterRestore getCustomResourceRestore(
      ClusterRestore source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.sgcluster.ClusterRestore transformation =
        new io.stackgres.common.crd.sgcluster.ClusterRestore();
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setBackupUid(source.getBackupUid());
    return transformation;
  }

  private StackGresClusterDistributedLogs getCustomResourceDistributedLogs(
      ClusterDistributedLogs source) {
    if (source == null) {
      return null;
    }
    StackGresClusterDistributedLogs transformation =
        new StackGresClusterDistributedLogs();
    transformation.setDistributedLogs(source.getDistributedLogs());
    return transformation;
  }

  public ClusterSpec getResourceSpec(StackGresClusterSpec source) {
    if (source == null) {
      return null;
    }
    ClusterSpec transformation = new ClusterSpec();
    transformation.setConfigurations(new ClusterConfiguration());
    transformation.getConfigurations().setSgBackupConfig(
        source.getConfiguration().getBackupConfig());
    transformation.getConfigurations().setSgPoolingConfig(source
        .getConfiguration().getConnectionPoolingConfig());
    transformation.setInstances(source.getInstances());
    transformation.setNonProduction(
        getResourceNonProduction(source.getNonProduction()));
    transformation.getConfigurations().setSgPostgresConfig(
        source.getConfiguration().getPostgresConfig());
    transformation.setPostgresVersion(source.getPostgresVersion());
    transformation.setPrometheusAutobind(source.getPrometheusAutobind());
    transformation.setSgInstanceProfile(source.getResourceProfile());

    Optional.ofNullable(source.getInitData())
        .map(StackGresClusterInitData::getRestore)
        .ifPresent(clusterRestore -> {
          transformation.setInitData(new ClusterInitData());
          transformation.getInitData().setRestore(
              getResourceRestore(source.getInitData().getRestore()));
        });

    final ClusterPod targetPod = new ClusterPod();
    final StackGresClusterPod sourcePod = source.getPod();

    transformation.setPods(targetPod);
    targetPod.setPersistentVolume(new ClusterPodPersistentVolume());
    targetPod.getPersistentVolume().setStorageClass(
        sourcePod.getPersistentVolume().getStorageClass());
    targetPod.getPersistentVolume().setVolumeSize(
        sourcePod.getPersistentVolume().getVolumeSize());
    targetPod
        .setDisableConnectionPooling(sourcePod.getDisableConnectionPooling());
    targetPod
        .setDisableMetricsExporter(sourcePod.getDisableMetricsExporter());
    targetPod
        .setDisablePostgresUtil(sourcePod.getDisablePostgresUtil());

    targetPod.setMetadata(Optional.ofNullable(sourcePod.getMetadata()).map(sourcePodMetadata -> {
      ClusterPodMetadata clusterPodMetadata = new ClusterPodMetadata();
      clusterPodMetadata.setAnnotations(sourcePodMetadata.getAnnotations());
      clusterPodMetadata.setLabels(sourcePodMetadata.getLabels());
      return clusterPodMetadata;
    }).orElse(null));

    transformation.setDistributedLogs(
        getResourceDistributedLogs(source.getDistributedLogs()));

    return transformation;
  }

  private NonProduction getResourceNonProduction(
      io.stackgres.common.crd.sgcluster.NonProduction source) {
    if (source == null) {
      return null;
    }
    NonProduction transformation = new NonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private ClusterRestore getResourceRestore(
      io.stackgres.common.crd.sgcluster.ClusterRestore source) {
    if (source == null) {
      return null;
    }
    ClusterRestore transformation = new ClusterRestore();
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setBackupUid(source.getBackupUid());
    return transformation;
  }

  private ClusterDistributedLogs getResourceDistributedLogs(
      StackGresClusterDistributedLogs source) {
    if (source == null) {
      return null;
    }
    ClusterDistributedLogs transformation = new ClusterDistributedLogs();
    transformation.setDistributedLogs(source.getDistributedLogs());
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
