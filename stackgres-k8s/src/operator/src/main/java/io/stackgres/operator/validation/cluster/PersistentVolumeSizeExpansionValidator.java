/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
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
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CLUSTER_UPDATE)
public class PersistentVolumeSizeExpansionValidator implements ClusterValidator {

  private final ResourceFinder<StorageClass> finder;

  private final ResourceScanner<PersistentVolumeClaim> pvcScanner;

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public PersistentVolumeSizeExpansionValidator(
      ResourceFinder<StorageClass> finder,
      ResourceScanner<PersistentVolumeClaim> pvcScanner,
      LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.finder = finder;
    this.pvcScanner = pvcScanner;
    this.labelFactory = labelFactory;
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {

    if (isOperationUpdate(review) && isVolumeSizeExpanded(review)) {

      List<StorageClass> storageClasses = findClusterStorageClasses(review);
      if (storageClasses.isEmpty()){
        /*
         * No storage classes means that there are not PVC created, therefore we can't verify if
         * the storage class support volume expansion
         */
        fail("Cannot increase persistent volume size because we cannot verify if the storage class "
            + "allows it, try again later");
      }

      // We need to be sure that all storage classes used by the cluster support volume expansion
      for (StorageClass storageClass : storageClasses) {
        if (!doesStorageClassAllowsExpansion(storageClass)){
          fail("Cannot increase persistent volume size because the storage class "
              + storageClass.getMetadata().getName() + " doesn't allows it");
        }
      }
    }

  }

  private List<StorageClass> findClusterStorageClasses(StackGresClusterReview review) {
    return getStorageClassName(review)
        .map(finder::findByName)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(List::of)
        .orElseGet(() -> {
          /*
           * If we are here, is because there is no storage class configured,
           * therefore we have to look for the cluster PVCs
           */
          final StackGresCluster stackGresCluster = review.getRequest().getObject();
          String clusterNamespace = stackGresCluster.getMetadata().getNamespace();
          Map<String, String> clusterLabels = labelFactory.clusterLabels(stackGresCluster);
          List<PersistentVolumeClaim> pvcs = pvcScanner
              .findByLabelsAndNamespace(clusterNamespace, clusterLabels);
          return pvcs.stream()
              .map(pvc -> pvc.getSpec().getStorageClassName())
              .distinct()
              // Since is very likely that all the storage classes
              // are the same we should look only for the different ones to avoid unneeded requests
              .map(finder::findByName)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toUnmodifiableList());
        });

  }

  private boolean doesStorageClassAllowsExpansion(StorageClass storageClass) {
    return Optional.of(storageClass)
        .map(StorageClass::getAllowVolumeExpansion)
        .orElse(false);
  }

  private Optional<String> getStorageClassName(StackGresClusterReview review) {
    return Optional.of(review.getRequest().getObject())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getPersistentVolume)
        .map(StackGresPodPersistentVolume::getStorageClass);
  }

  private boolean isOperationUpdate(StackGresClusterReview review) {
    return review.getRequest().getOperation() == Operation.UPDATE;
  }

  private boolean isVolumeSizeExpanded(StackGresClusterReview review) {
    var oldVolumeSize = review.getRequest().getOldObject().getSpec().getPod()
        .getPersistentVolume().getSize();
    var newVolumeSize = review.getRequest().getObject().getSpec().getPod()
        .getPersistentVolume().getSize();

    var oldSizeQuantity = new Quantity(oldVolumeSize);
    var newSizeQuantity = new Quantity(newVolumeSize);

    var oldSizeInBytes = Quantity.getAmountInBytes(oldSizeQuantity);
    var newSizeInBytes = Quantity.getAmountInBytes(newSizeQuantity);

    return newSizeInBytes.compareTo(oldSizeInBytes) > 0;

  }

}
