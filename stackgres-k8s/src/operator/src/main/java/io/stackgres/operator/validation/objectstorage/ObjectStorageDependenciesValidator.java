/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class ObjectStorageDependenciesValidator
    extends DependenciesValidator<StackGresObjectStorageReview, StackGresCluster>
    implements ObjectStorageValidator {

  @Override
  protected void validate(StackGresObjectStorageReview review, StackGresCluster resource)
      throws ValidationFailed {
    var backupsConfigurationOpt = Optional.ofNullable(resource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBackups);

    if (backupsConfigurationOpt.isPresent()) {
      var backupsConfiguration = backupsConfigurationOpt.orElseThrow();
      var storageObjectName = review.getRequest().getName();

      boolean isObjectStorageReferenced = backupsConfiguration.stream()
          .anyMatch(bc -> Objects.equals(bc.getSgObjectStorage(), storageObjectName));

      if (isObjectStorageReferenced) {
        fail(review, resource);
      }
    }
  }

  @Override
  protected Class<StackGresCluster> getResourceClass() {
    return StackGresCluster.class;
  }

}
