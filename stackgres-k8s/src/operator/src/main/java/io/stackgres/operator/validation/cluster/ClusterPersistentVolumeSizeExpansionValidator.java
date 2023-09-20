/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

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
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jetbrains.annotations.NotNull;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_UPDATE)
public class ClusterPersistentVolumeSizeExpansionValidator
    extends PersistentVolumeSizeExpansionValidator<StackGresClusterReview,
        StackGresCluster, StackGresCluster>
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
  protected @NotNull String getVolumeSize(StackGresCluster cluster) {
    return cluster.getSpec().getPods().getPersistentVolume().getSize();
  }

  @Override
  protected Optional<String> getStorageClass(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getPersistentVolume)
        .map(StackGresPodPersistentVolume::getStorageClass);
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
    return labelFactory;
  }

  @Override
  protected List<StackGresCluster> getClusters(StackGresCluster resource) {
    return List.of(resource);
  }

  @Override
  protected ResourceScanner<PersistentVolumeClaim> getPvcScanner() {
    return pvcScanner;
  }
}
