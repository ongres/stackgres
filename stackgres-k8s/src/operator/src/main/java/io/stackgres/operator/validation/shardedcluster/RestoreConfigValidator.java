/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Objects;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterInitialData;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestore;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class RestoreConfigValidator
    implements ShardedClusterValidator {

  private final String errorConstraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    StackGresShardedCluster cluster = review.getRequest().getObject();

    Optional<StackGresShardedClusterRestore> restoreOpt = Optional.ofNullable(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getInitialData)
        .map(StackGresShardedClusterInitialData::getRestore);

    checkRestoreConfig(review, restoreOpt);

    if (restoreOpt.isPresent()) {
      StackGresShardedClusterRestore restoreConfig = restoreOpt.get();
      checkBackup(review, restoreConfig);
    }
  }

  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  private void checkBackup(
      StackGresShardedClusterReview review,
      StackGresShardedClusterRestore restoreConfig) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        if (restoreConfig.getFromBackup() == null) {
          break;
        }
        break;
      case UPDATE:
        StackGresShardedClusterRestore oldRestoreConfig =
            Optional.of(review.getRequest().getOldObject().getSpec())
            .map(StackGresShardedClusterSpec::getInitialData)
            .map(StackGresShardedClusterInitialData::getRestore)
            .orElse(null);

        final String message = "Cannot update SGShardedCluster's restore configuration";
        if (!Objects.equals(restoreConfig, oldRestoreConfig)) {
          fail(errorConstraintViolationUri, message);
        }
        break;
      default:
    }
  }

  private void checkRestoreConfig(
      StackGresShardedClusterReview review,
      Optional<StackGresShardedClusterRestore> initRestoreOpt) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      Optional<StackGresShardedClusterRestore> oldRestoreOpt = Optional
          .ofNullable(review.getRequest().getOldObject().getSpec().getInitialData())
          .map(StackGresShardedClusterInitialData::getRestore);

      if (initRestoreOpt.isEmpty() && oldRestoreOpt.isPresent()) {
        fail(errorConstraintViolationUri, "Cannot update SGShardedCluster's restore configuration");
      }
    }
  }

}
