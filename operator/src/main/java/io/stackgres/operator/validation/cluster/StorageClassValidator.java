/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.inject.Inject;

import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.services.StorageClassFinder;
import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.ValidationFailed;

public class StorageClassValidator implements ClusterValidator {

  private StorageClassFinder finder;

  @Inject
  public StorageClassValidator(StorageClassFinder finder) {
    this.finder = finder;
  }

  @Override
  public void validate(AdmissionReview review) throws ValidationFailed {

    StackGresCluster cluster = review.getRequest().getObject();
    String storageClass = cluster.getSpec().getStorageClass();

    switch (review.getRequest().getOperation()) {
      case CREATE:
        checkIfStorageClassExist(storageClass, "Storage class "
            + storageClass + " not found");
        break;
      case UPDATE:
        checkIfStorageClassExist(storageClass, "Cannot update to storage class "
            + storageClass + " because it doesn't exists");
        break;
      default:
    }

  }

  private void checkIfStorageClassExist(String storageClass, String onError)
      throws ValidationFailed {
    if (!finder.findStorageClass(storageClass).isPresent()) {
      throw new ValidationFailed(onError);
    }
  }
}
