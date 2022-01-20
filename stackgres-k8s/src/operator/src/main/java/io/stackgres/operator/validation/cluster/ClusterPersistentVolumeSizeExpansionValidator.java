/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.common.ErrorType;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jetbrains.annotations.NotNull;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_UPDATE)
public class ClusterPersistentVolumeSizeExpansionValidator
    extends PersistentVolumeSizeExpansionValidator<StackGresClusterReview, StackGresCluster>
    implements ClusterValidator {

  private final ResourceFinder<StorageClass> finder;

  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public ClusterPersistentVolumeSizeExpansionValidator(
      ResourceFinder<StorageClass> finder,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.finder = finder;
    this.pvcScanner = pvcScanner;
    this.labelFactory = labelFactory;
  }

  @Override
  public @NotNull String getVolumeSize(StackGresCluster cluster) {
    return cluster.getSpec().getPod().getPersistentVolume().getSize();
  }

  @Override
  public Optional<String> getStorageClass(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getPersistentVolume)
        .map(StackGresPodPersistentVolume::getStorageClass);
  }

  @Override
  public boolean isOperationUpdate(StackGresClusterReview review) {
    return review.getRequest().getOperation() == Operation.UPDATE;
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
    return labelFactory;
  }

  @Override
  public ResourceScanner<PersistentVolumeClaim> getPvcScanner() {
    return pvcScanner;
  }
}
