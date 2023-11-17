/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class DistributedLogsValidator
    extends AbstractReferenceValidator<
      StackGresCluster, StackGresClusterReview, StackGresDistributedLogs>
    implements ClusterValidator {

  @Inject
  public DistributedLogsValidator(
      CustomResourceFinder<StackGresDistributedLogs> distributedLogsFinder) {
    super(distributedLogsFinder);
  }

  @Override
  protected Class<StackGresDistributedLogs> getReferenceClass() {
    return StackGresDistributedLogs.class;
  }

  @Override
  protected String getReference(StackGresCluster resource) {
    return Optional.ofNullable(resource.getSpec().getDistributedLogs())
        .map(StackGresClusterDistributedLogs::getSgDistributedLogs)
        .map(StackGresUtil::getNameFromRelativeId)
        .orElse(null);
  }

  @Override
  protected String getReferenceNamespace(StackGresCluster resource) {
    return Optional.ofNullable(resource.getSpec().getDistributedLogs())
        .map(StackGresClusterDistributedLogs::getSgDistributedLogs)
        .map(reference -> StackGresUtil.getNamespaceFromRelativeId(
            reference, resource.getMetadata().getNamespace()))
        .orElse(null);
  }

  @Override
  protected void onNotFoundReference(String message) throws ValidationFailed {
    fail(message);
  }

}
