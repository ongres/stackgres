/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ObjectStorageValidator
    extends AbstractReferenceValidator<
      StackGresCluster, StackGresClusterReview, StackGresObjectStorage>
    implements ClusterValidator {

  @Inject
  public ObjectStorageValidator(CustomResourceFinder<StackGresObjectStorage> objectStorageFinder) {
    super(objectStorageFinder);
  }

  @Override
  protected Class<StackGresObjectStorage> getReferenceClass() {
    return StackGresObjectStorage.class;
  }

  @Override
  protected String getReference(StackGresCluster resource) {
    return Optional.ofNullable(resource.getSpec().getConfigurations())
        .map(StackGresClusterConfigurations::getBackups)
        .filter(Predicate.not(List::isEmpty))
        .map(backups -> backups.get(0))
        .map(StackGresClusterBackupConfiguration::getSgObjectStorage)
        .orElse(null);
  }

  @Override
  protected void onNotFoundReference(String message) throws ValidationFailed {
    fail(message);
  }

}
