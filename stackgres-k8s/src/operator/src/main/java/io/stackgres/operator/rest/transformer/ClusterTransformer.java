/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterRestore;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.rest.dto.cluster.ClusterDto;
import io.stackgres.operator.rest.dto.cluster.ClusterRestore;
import io.stackgres.operator.rest.dto.cluster.ClusterSpec;
import io.stackgres.operator.rest.dto.cluster.NonProduction;

@ApplicationScoped
public class ClusterTransformer
    extends AbstractResourceTransformer<ClusterDto, StackGresCluster> {

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
    return transformation;
  }

  public StackGresClusterSpec getCustomResourceSpec(ClusterSpec source) {
    StackGresClusterSpec transformation = new StackGresClusterSpec();
    transformation.setBackupConfig(source.getBackupConfig());
    transformation.setConnectionPoolingConfig(source.getConnectionPoolingConfig());
    transformation.setInstances(source.getInstances());
    transformation.setNonProduction(
        getCustomResourceNonProduction(source.getNonProduction()));
    transformation.setPostgresConfig(source.getPostgresConfig());
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
    io.stackgres.operator.customresource.sgcluster.NonProduction transformation =
        new io.stackgres.operator.customresource.sgcluster.NonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private StackGresClusterRestore getCustomResourceRestore(ClusterRestore source) {
    StackGresClusterRestore transformation = new StackGresClusterRestore();
    transformation.setAutoCopySecretsEnabled(source.isAutoCopySecretsEnabled());
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setStackgresBackup(source.getStackgresBackup());
    return transformation;
  }

  public ClusterSpec getResourceSpec(StackGresClusterSpec source) {
    ClusterSpec transformation = new ClusterSpec();
    transformation.setBackupConfig(source.getBackupConfig());
    transformation.setConnectionPoolingConfig(source.getConnectionPoolingConfig());
    transformation.setInstances(source.getInstances());
    transformation.setNonProduction(
        getResourceNonProduction(source.getNonProduction()));
    transformation.setPostgresConfig(source.getPostgresConfig());
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
    NonProduction transformation = new NonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private ClusterRestore getResourceRestore(StackGresClusterRestore source) {
    ClusterRestore transformation = new ClusterRestore();
    transformation.setAutoCopySecretsEnabled(source.isAutoCopySecretsEnabled());
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setStackgresBackup(source.getStackgresBackup());
    return transformation;
  }

}
