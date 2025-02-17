/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Objects;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitialData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class RestoreConfigValidator
    implements ClusterValidator {

  private final String errorConstraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();

    Optional<StackGresClusterRestore> restoreOpt = Optional.ofNullable(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getInitialData)
        .map(StackGresClusterInitialData::getRestore);

    checkRestoreConfig(review, restoreOpt);

    if (restoreOpt.isPresent()) {
      StackGresClusterRestore restoreConfig = restoreOpt.get();
      checkBackup(review, restoreConfig);
    }
  }

  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  private void checkBackup(
      StackGresClusterReview review,
      StackGresClusterRestore restoreConfig) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
        if (restoreConfig.getFromBackup() == null) {
          break;
        }
        break;
      case UPDATE:
        StackGresClusterRestore oldRestoreConfig =
            Optional.of(review.getRequest().getOldObject().getSpec())
            .map(StackGresClusterSpec::getInitialData)
            .map(StackGresClusterInitialData::getRestore)
            .orElse(null);

        final String message = "Cannot update SGCluster's restore configuration";
        if (!Objects.equals(restoreConfig, oldRestoreConfig)) {
          fail(errorConstraintViolationUri, message);
        }
        break;
      default:
    }
  }

  private void checkRestoreConfig(
      StackGresClusterReview review,
      Optional<StackGresClusterRestore> initRestoreOpt) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      Optional<StackGresClusterRestore> oldRestoreOpt = Optional
          .ofNullable(review.getRequest().getOldObject().getSpec().getInitialData())
          .map(StackGresClusterInitialData::getRestore);

      if (initRestoreOpt.isEmpty() && oldRestoreOpt.isPresent()) {
        fail(errorConstraintViolationUri, "Cannot update SGCluster's restore configuration");
      }
    }
  }

}
