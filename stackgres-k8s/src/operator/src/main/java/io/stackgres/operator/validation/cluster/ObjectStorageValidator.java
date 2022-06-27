/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ObjectStorageValidator implements ClusterValidator {

  private final CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  @Inject
  public ObjectStorageValidator(CustomResourceFinder<StackGresObjectStorage> objectStorageFinder) {
    this.objectStorageFinder = objectStorageFinder;
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    Operation operation = review.getRequest().getOperation();
    if (operation == Operation.CREATE) {
      checkIfObjectStorageExists(review, "Object storage %s not found");
    } else if (operation == Operation.UPDATE) {
      checkIfObjectStorageExists(review, "Cannot update to object storage %s"
          + " because it doesn't exists");
    }
  }

  private void checkIfObjectStorageExists(StackGresClusterReview review, String onError)
      throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();
    String namespace = cluster.getMetadata().getNamespace();
    List<StackGresClusterBackupConfiguration> backupsConfigs =
        cluster.getSpec().getConfiguration().getBackups();

    if (backupsConfigs != null) {
      for (StackGresClusterBackupConfiguration backup : backupsConfigs) {
        String objectStorage = backup.getObjectStorage();
        if (objectStorage != null) {
          Optional<StackGresObjectStorage> backupConfigOpt = objectStorageFinder
              .findByNameAndNamespace(objectStorage, namespace);
          if (backupConfigOpt.isEmpty()) {
            fail(onError.formatted(objectStorage));
          }
        }
      }
    }
  }

}
