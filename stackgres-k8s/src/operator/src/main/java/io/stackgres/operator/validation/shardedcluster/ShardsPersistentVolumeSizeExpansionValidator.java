/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jetbrains.annotations.NotNull;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_UPDATE)
public class ShardsPersistentVolumeSizeExpansionValidator
    extends PersistentVolumeSizeExpansionValidator<StackGresShardedClusterReview,
        StackGresShardedCluster, StackGresCluster>
    implements ShardedClusterValidator {

  private final ResourceFinder<StorageClass> finder;

  private final CustomResourceScanner<StackGresCluster> clusterScanner;

  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  private final LabelFactoryForShardedCluster labelFactory;

  private final LabelFactoryForCluster<StackGresCluster> clusterLabelFactory;

  @Inject
  public ShardsPersistentVolumeSizeExpansionValidator(
      ResourceFinder<StorageClass> finder,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      LabelFactoryForShardedCluster labelFactory,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      LabelFactoryForCluster<StackGresCluster> clusterLabelFactory) {
    this.finder = finder;
    this.clusterScanner = clusterScanner;
    this.labelFactory = labelFactory;
    this.pvcScanner = pvcScanner;
    this.clusterLabelFactory = clusterLabelFactory;
  }

  @Override
  public @NotNull String getVolumeSize(StackGresShardedCluster cluster) {
    return cluster.getSpec().getShards().getPods().getPersistentVolume().getSize();
  }

  @Override
  public Optional<String> getStorageClass(StackGresShardedCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getShards)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getPersistentVolume)
        .map(StackGresPodPersistentVolume::getStorageClass);
  }

  @Override
  public void throwValidationError(String message) throws ValidationFailed {
    fail(message);
  }

  @Override
  public ResourceFinder<StorageClass> getStorageClassFinder() {
    return finder;
  }

  @Override
  public LabelFactoryForCluster<StackGresCluster> getLabelFactory() {
    return clusterLabelFactory;
  }

  @Override
  protected List<StackGresCluster> getClusters(StackGresShardedCluster resource) {
    return clusterScanner.getResourcesWithLabels(
        resource.getMetadata().getNamespace(), labelFactory.shardsLabels(resource));
  }

  @Override
  public ResourceScanner<PersistentVolumeClaim> getPvcScanner() {
    return pvcScanner;
  }
}
