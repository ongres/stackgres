/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.KubernetesResourceFinder;
import io.stackgres.operator.validation.StackgresClusterReview;
import io.stackgres.operatorframework.ValidationFailed;

@ApplicationScoped
public class StorageClassValidator implements ClusterValidator {

  private KubernetesResourceFinder<StorageClass> finder;

  @Inject
  public StorageClassValidator(KubernetesResourceFinder<StorageClass> finder) {
    this.finder = finder;
  }

  @Override
  public void validate(StackgresClusterReview review) throws ValidationFailed {

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
    if (storageClass != null && !storageClass.isEmpty()
        && !finder.findByName(storageClass).isPresent()) {
      throw new ValidationFailed(onError);
    }
  }
}
