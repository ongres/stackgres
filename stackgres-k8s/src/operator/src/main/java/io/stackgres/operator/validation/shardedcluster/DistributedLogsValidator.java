/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.AbstractReferenceValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class DistributedLogsValidator
    extends AbstractReferenceValidator<
      StackGresShardedCluster, StackGresShardedClusterReview, StackGresDistributedLogs>
    implements ShardedClusterValidator {

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
  protected String getReference(StackGresShardedCluster resource) {
    return Optional.ofNullable(resource.getSpec().getDistributedLogs())
        .map(StackGresClusterDistributedLogs::getSgDistributedLogs)
        .map(StackGresUtil::getNameFromRelativeId)
        .orElse(null);
  }

  @Override
  protected String getReferenceNamespace(StackGresShardedCluster resource) {
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
