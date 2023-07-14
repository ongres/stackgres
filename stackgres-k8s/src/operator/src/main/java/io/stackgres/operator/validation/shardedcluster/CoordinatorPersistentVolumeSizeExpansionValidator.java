/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
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
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_UPDATE)
public class CoordinatorPersistentVolumeSizeExpansionValidator
    extends PersistentVolumeSizeExpansionValidator<StackGresShardedClusterReview,
        StackGresShardedCluster, StackGresCluster>
    implements ShardedClusterValidator {

  private final ResourceFinder<StorageClass> finder;

  private final CustomResourceScanner<StackGresCluster> clusterScanner;

  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  private final LabelFactoryForShardedCluster labelFactory;

  private final LabelFactoryForCluster<StackGresCluster> clusterLabelFactory;

  @Inject
  public CoordinatorPersistentVolumeSizeExpansionValidator(
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
  protected @NotNull String getVolumeSize(StackGresShardedCluster cluster) {
    return cluster.getSpec().getCoordinator().getPod().getPersistentVolume().getSize();
  }

  @Override
  protected Optional<String> getStorageClass(StackGresShardedCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getPersistentVolume)
        .map(StackGresPodPersistentVolume::getStorageClass);
  }

  @Override
  protected boolean isOperationUpdate(StackGresShardedClusterReview review) {
    return review.getRequest().getOperation() == Operation.UPDATE;
  }

  @Override
  protected void throwValidationError(String message) throws ValidationFailed {
    fail(message);
  }

  @Override
  protected ResourceFinder<StorageClass> getStorageClassFinder() {
    return finder;
  }

  @Override
  protected LabelFactoryForCluster<StackGresCluster> getLabelFactory() {
    return clusterLabelFactory;
  }

  @Override
  protected List<StackGresCluster> getClusters(StackGresShardedCluster resource) {
    return clusterScanner.getResourcesWithLabels(
        resource.getMetadata().getNamespace(), labelFactory.coordinatorLabels(resource));
  }

  @Override
  protected ResourceScanner<PersistentVolumeClaim> getPvcScanner() {
    return pvcScanner;
  }
}
