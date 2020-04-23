/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.resource.ResourceFinder;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_STORAGE_CLASS)
public class StorageClassValidator implements DistributedLogsValidator {

  private ResourceFinder<StorageClass> finder;

  private ConfigContext context;

  @Inject
  public StorageClassValidator(ResourceFinder<StorageClass> finder, ConfigContext context) {
    this.finder = finder;
    this.context = context;
  }

  @Override
  public void validate(StackGresDistributedLogsReview review) throws ValidationFailed {

    StackGresDistributedLogs cluster = review.getRequest().getObject();
    String storageClass = cluster.getSpec().getPersistentVolume().getStorageClass();

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
      fail(context, onError);
    }
  }
}
