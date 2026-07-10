/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.CustomPersistentVolumeUtil;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.AbstractCustomPersistentVolumesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class CustomPersistentVolumesValidator extends AbstractCustomPersistentVolumesValidator
    implements ClusterValidator {

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return;
    }
    final StackGresCluster cluster = review.getRequest().getObject();
    final Optional<StackGresCluster> oldCluster =
        review.getRequest().getOperation() == Operation.UPDATE
        ? Optional.ofNullable(review.getRequest().getOldObject())
        : Optional.empty();
    final List<String> appliedWalPaths = oldCluster
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPodStatuses)
        .stream()
        .flatMap(List::stream)
        .map(StackGresClusterPodStatus::getWalPath)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    validateCustomPersistentVolumes(
        Optional.ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getPods)
            .orElse(null),
        CustomPersistentVolumeUtil.getWalPath(cluster),
        oldCluster
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getPods),
        oldCluster
            .flatMap(CustomPersistentVolumeUtil::getWalPath),
        appliedWalPaths,
        ".spec");
  }

  @Override
  protected void failValidation(String message, String... fields) throws ValidationFailed {
    failWithFields(message, fields);
  }

}
