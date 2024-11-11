/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsCluster;
import io.stackgres.operator.validation.PersistentVolumeSizeExpansionValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_UPDATE)
public class DistributedLogsPersistentVolumeSizeExpansionValidator
    extends PersistentVolumeSizeExpansionValidator<StackGresDistributedLogsReview, StackGresDistributedLogs>
    implements DistributedLogsValidator {

  private final ResourceFinder<StorageClass> finder;

  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  private final LabelFactoryForDistributedLogs labelFactory;

  private final LabelFactoryForCluster labelFactoryForCluster;

  @Inject
  public DistributedLogsPersistentVolumeSizeExpansionValidator(
      ResourceFinder<StorageClass> finder,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      LabelFactoryForDistributedLogs labelFactory,
      LabelFactoryForCluster labelFactoryForCluster) {
    this.finder = finder;
    this.pvcScanner = pvcScanner;
    this.labelFactory = labelFactory;
    this.labelFactoryForCluster = labelFactoryForCluster;
  }

  @Override
  protected @NotNull String getVolumeSize(StackGresDistributedLogs cluster) {
    return cluster.getSpec().getPersistentVolume().getSize();
  }

  @Override
  protected Optional<String> getStorageClass(StackGresDistributedLogs cluster) {
    return Optional.of(cluster)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getPersistentVolume)
        .map(StackGresClusterPodsPersistentVolume::getStorageClass);
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
  protected LabelFactoryForCluster getLabelFactory() {
    return labelFactoryForCluster;
  }

  @Override
  protected List<StackGresCluster> getClusters(StackGresDistributedLogs resource) {
    return List.of(DistributedLogsCluster.getCluster(labelFactory, resource, Optional.empty()));
  }

  @Override
  protected ResourceScanner<PersistentVolumeClaim> getPvcScanner() {
    return pvcScanner;
  }
}
