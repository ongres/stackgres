/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public abstract class PersistentVolumeSizeExpansionValidator<T extends AdmissionReview<R>,
    R extends CustomResource<?, ?>, S extends CustomResource<?, ?>> implements Validator<T> {

  private final boolean clusterRoleDisabled = OperatorProperty.CLUSTER_ROLE_DISABLED.getBoolean();

  @Override
  public void validate(T review) throws ValidationFailed {
    if (isOperationUpdate(review) && compareVolumeSizes(review) != 0) {
      if (compareVolumeSizes(review) < 0) {
        // At the moment we can't decrease volume sizes
        throwValidationError("Decrease of persistent volume size is not supported");
      }

      //If we are here is because the persistent volume size is being increased
      if (!clusterRoleDisabled) {
        List<StorageClass> storageClasses = findClusterStorageClasses(review);
        if (storageClasses.isEmpty()) {
          /*
           * No storage classes means that there are not PVC created, therefore we can't verify if
           * the storage class support volume expansion
           */
          throwValidationError("Cannot increase persistent volume size because we cannot verify if "
              + "the storage class allows it, try again later");
        }

        // We need to be sure that all storage classes used by the cluster support volume expansion
        for (StorageClass storageClass : storageClasses) {
          if (!doesStorageClassAllowsExpansion(storageClass)) {
            throwValidationError("Cannot increase persistent volume size because the storage class "
                + storageClass.getMetadata().getName() + " doesn't allows it");
          }
        }
      }
    }
  }

  /**
   * Thorws a validation failed exception with the given message.
   *
   * @param message the error message
   * @throws ValidationFailed always throws this exception
   */
  protected abstract void throwValidationError(String message) throws ValidationFailed;

  /**
   * Given a cluster it returns the configure persistent volume size.
   *
   * @param cluster the cluster to look into
   * @return the volume size
   */
  protected abstract String getVolumeSize(R cluster);

  /**
   * Given a cluster it returns the configured storage class if any.
   *
   * @param cluster the cluster to look
   * @return the configured storage class if any
   */
  protected abstract Optional<String> getStorageClass(R cluster);

  /**
   * Looks for a storage class finder.
   *
   * @return a storage class finder.
   */
  protected abstract ResourceFinder<StorageClass> getStorageClassFinder();

  /**
   * Looks for a LabelFactoryForCluster.
   *
   * @return a label factory for cluster.
   */
  protected abstract LabelFactoryForCluster<S> getLabelFactory();

  /**
   * Looks for a PersistentVolumeClaim scanner.
   *
   * @return a Persistent Volume Claim scanner.
   */
  protected abstract ResourceScanner<PersistentVolumeClaim> getPvcScanner();

  /**
   * Given an admission review it returns the storage class name configured.
   *
   * @param review the admission review
   * @return the configured storage class name if any
   */
  private Optional<String> getStorageClassName(T review) {
    R targetCluster = review.getRequest().getObject();
    return getStorageClass(targetCluster);
  }

  /**
   * Finds the storage classes for an admission review.
   *
   * @param review an update admission review
   * @return the list of storage classes
   */
  protected List<StorageClass> findClusterStorageClasses(T review) {
    return getStorageClassName(review)
        .map(getStorageClassFinder()::findByName)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(List::of)
        .orElseGet(() -> {
          /*
           * If we are here, is because there is no storage class configured,
           * therefore we have to look for the cluster PVCs
           */
          final List<S> clusters = getClusters(review.getRequest().getObject());
          return clusters.stream()
              .flatMap(cluster -> {
                String clusterNamespace = cluster.getMetadata().getNamespace();
                Map<String, String> clusterLabels = getLabelFactory().clusterLabels(cluster);
                List<PersistentVolumeClaim> pvcs = getPvcScanner()
                    .getResourcesInNamespaceWithLabels(clusterNamespace, clusterLabels);
                return pvcs.stream()
                    .map(pvc -> pvc.getSpec().getStorageClassName())
                    .distinct()
                    // Since is very likely that all the storage classes are the same
                    // we should look only for the different ones to avoid unneeded requests
                    .map(getStorageClassFinder()::findByName)
                    .flatMap(Optional::stream);
              })
              .toList();
        });
  }

  protected abstract List<S> getClusters(R resource);

  /**
   * Checks if a storage class allows volume expansion.
   *
   * @param storageClass the storage class to set
   * @return <code>true</code> if the storage class allows expansion, <code>false</code> otherwise
   */
  protected boolean doesStorageClassAllowsExpansion(StorageClass storageClass) {
    return Optional.of(storageClass)
        .map(StorageClass::getAllowVolumeExpansion)
        .orElse(false);
  }

  /**
   * Check if the operation is an update operation for a given review.
   *
   * @param review the review to check
   * @return <code>if the operation is update</code>, <code>false</code> otherwise
   */
  private boolean isOperationUpdate(T review) {
    return review.getRequest().getOperation() == Operation.UPDATE;
  }

  /**
   * Given an update admission review it compares the persistent volume sizes of the old and new
   * objects.
   *
   * @param review The update admission review
   * @return <code>1</code> if the new persistent volume sizes is bigger than the old one,
   *         <code>0</code> if the persistent volume size is equal
   *         <code>-1</code> if the new persistent volume size is lower than the old one
   */
  protected int compareVolumeSizes(T review) {
    R oldObject = review.getRequest().getOldObject();
    R newObject = review.getRequest().getObject();

    var oldVolumeSize = getVolumeSize(oldObject);
    var newVolumeSize = getVolumeSize(newObject);

    var oldSizeQuantity = new Quantity(oldVolumeSize);
    var newSizeQuantity = new Quantity(newVolumeSize);

    var oldSizeInBytes = Quantity.getAmountInBytes(oldSizeQuantity);
    var newSizeInBytes = Quantity.getAmountInBytes(newSizeQuantity);

    return newSizeInBytes.compareTo(oldSizeInBytes);
  }

}
