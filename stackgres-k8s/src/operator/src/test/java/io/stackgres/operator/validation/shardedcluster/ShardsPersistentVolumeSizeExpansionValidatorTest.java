/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.stackgres.common.StackGresShardedClusterForCitusUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidator;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardsPersistentVolumeSizeExpansionValidatorTest extends
    PersistentVolumeSizeExpansionValidatorTest<StackGresShardedClusterReview,
        StackGresShardedCluster, StackGresCluster> {

  @Mock
  protected CustomResourceScanner<StackGresCluster> clusterScanner;

  @Mock
  protected LabelFactoryForShardedCluster shardedClusterLabelFactory;

  @Override
  protected StackGresShardedClusterReview getAdmissionReview() {
    return AdmissionReviewFixtures.shardedCluster().loadUpdate().get();
  }

  @Override
  protected PersistentVolumeSizeExpansionValidator<StackGresShardedClusterReview,
      StackGresShardedCluster, StackGresCluster> getValidator() {
    return new ShardsPersistentVolumeSizeExpansionValidator(
        finder, clusterScanner, shardedClusterLabelFactory, pvcScanner, labelFactory);
  }

  @Override
  protected StackGresCluster getCluster(StackGresShardedCluster resource) {
    return StackGresShardedClusterForCitusUtil.getShardsCluster(resource, 0);
  }

  @Override
  protected void setVolumeSize(StackGresShardedCluster cluster, String size) {
    cluster.getSpec().getShards().getPods().getPersistentVolume().setSize(size);
  }

  @Override
  protected void setStorageClassName(StackGresShardedCluster cluster, String storageClassName) {
    cluster.getSpec().getShards()
        .getPods().getPersistentVolume().setStorageClass(storageClassName);
  }

  @Override
  protected String getStorageClassName(StackGresShardedCluster cluster) {
    return cluster.getSpec().getShards().getPods().getPersistentVolume().getStorageClass();
  }

  @Override
  protected void configureEmptyPvcScanner(StackGresShardedCluster resource) {
    StackGresCluster cluster = getCluster(resource);
    when(clusterScanner.getResourcesWithLabels(any(), any()))
        .thenReturn(List.of(cluster));
    super.configureEmptyPvcScanner(resource);
  }

  @Override
  protected void configurePvcScanner(StackGresShardedCluster resource, String storageClassName,
      Map<String, String> clusterLabels, String clusterNamespace) {
    StackGresCluster cluster = getCluster(resource);
    when(clusterScanner.getResourcesWithLabels(any(), any()))
        .thenReturn(List.of(cluster));
    super.configurePvcScanner(resource, storageClassName, clusterLabels, clusterNamespace);
  }

  @Override
  protected void configureMixedPvcScanner(
      StackGresShardedCluster resource,
      Map<String, String> clusterLabels, String clusterNamespace,
      String expandableStorageClassName, String nonExpandableStorageClassName) {
    StackGresCluster cluster = getCluster(resource);
    when(clusterScanner.getResourcesWithLabels(any(), any()))
        .thenReturn(List.of(cluster));
    super.configureMixedPvcScanner(resource,
        clusterLabels, clusterNamespace, expandableStorageClassName,
        nonExpandableStorageClassName);
  }

}
