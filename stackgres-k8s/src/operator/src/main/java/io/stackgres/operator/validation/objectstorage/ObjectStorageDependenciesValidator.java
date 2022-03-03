/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.validation.DependenciesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_DELETION)
public class ObjectStorageDependenciesValidator extends DependenciesValidator<ObjectStorageReview> {

  @Override
  protected void validate(ObjectStorageReview review, StackGresCluster i) throws ValidationFailed {
    var backupsConfigurationOpt = Optional.of(i.getSpec())
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getBackups);

    if (backupsConfigurationOpt.isPresent()) {
      var backupsConfiguration = backupsConfigurationOpt.get();
      var storageObjectName = review.getRequest().getName();

      boolean isObjectStorageReferenced = backupsConfiguration.stream().anyMatch(
          bc -> Objects.equals(bc.getObjectStorage(), storageObjectName)
      );

      if (isObjectStorageReferenced) {
        fail(review, i);
      }
    }
  }
}
