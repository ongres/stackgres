/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.CustomPersistentVolumeUtil;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorkers;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.AbstractCustomPersistentVolumesValidator;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class CustomPersistentVolumesValidator extends AbstractCustomPersistentVolumesValidator
    implements ShardedClusterValidator {

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return;
    }
    final StackGresShardedClusterSpec spec = Optional
        .ofNullable(review.getRequest().getObject())
        .map(StackGresShardedCluster::getSpec)
        .orElse(null);
    if (spec == null) {
      return;
    }
    final Optional<StackGresShardedClusterSpec> oldSpec =
        review.getRequest().getOperation() == Operation.UPDATE
        ? Optional.ofNullable(review.getRequest().getOldObject())
            .map(StackGresShardedCluster::getSpec)
        : Optional.empty();

    // The sequencing check against the WAL paths applied to the Pods is enforced by the
    // SGCluster validation webhook when the operator propagates the changes to the generated
    // SGClusters.
    validateCustomPersistentVolumes(
        Optional.ofNullable(spec.getCoordinator())
            .map(StackGresClusterSpec::getPods)
            .orElse(null),
        Optional.ofNullable(spec.getCoordinator())
            .map(coordinator -> coordinator.getConfigurationsForCoordinator())
            .map(configurations -> configurations.getPostgres())
            .map(postgres -> postgres.getWalPath()),
        oldSpec.map(StackGresShardedClusterSpec::getCoordinator)
            .map(StackGresClusterSpec::getPods),
        oldSpec.map(StackGresShardedClusterSpec::getCoordinator)
            .map(coordinator -> coordinator.getConfigurationsForCoordinator())
            .map(configurations -> configurations.getPostgres())
            .map(postgres -> postgres.getWalPath()),
        List.of(),
        ".spec.coordinator");
    validateSection(
        Optional.ofNullable(spec.getWorkersOrShards()),
        oldSpec.map(StackGresShardedClusterSpec::getWorkersOrShards),
        ".spec.workers");

    final List<StackGresShardedClusterWorker> overrides = Optional
        .ofNullable(spec.getWorkersOrShards())
        .map(StackGresShardedClusterWorkers::getOverrides)
        .orElse(List.of());
    final List<StackGresShardedClusterWorker> oldOverrides = oldSpec
        .map(StackGresShardedClusterSpec::getWorkersOrShards)
        .map(StackGresShardedClusterWorkers::getOverrides)
        .orElse(List.of());
    for (int index = 0; index < overrides.size(); index++) {
      final StackGresShardedClusterWorker override = overrides.get(index);
      final Optional<StackGresShardedClusterWorker> oldOverride = oldOverrides.stream()
          .filter(oldWorker -> Objects.equals(
              oldWorker.getIndex(), override.getIndex()))
          .findFirst();
      validateCustomPersistentVolumes(
          override.getPodsForWorkers(),
          Optional.ofNullable(override.getConfigurationsForWorkers())
              .map(configurations -> configurations.getPostgres())
              .map(postgres -> postgres.getWalPath()),
          oldOverride.map(oldWorker -> (StackGresClusterPods) oldWorker.getPodsForWorkers()),
          oldOverride
              .map(StackGresShardedClusterWorker::getConfigurationsForWorkers)
              .map(configurations -> configurations.getPostgres())
              .map(postgres -> postgres.getWalPath()),
          List.of(),
          ".spec.workers.overrides[" + index + "]");
    }
  }

  private void validateSection(
      Optional<? extends StackGresClusterSpec> section,
      Optional<? extends StackGresClusterSpec> oldSection,
      String fieldPrefix) throws ValidationFailed {
    validateCustomPersistentVolumes(
        section
            .map(StackGresClusterSpec::getPods)
            .orElse(null),
        section
            .flatMap(CustomPersistentVolumeUtil::getWalPath),
        oldSection
            .map(StackGresClusterSpec::getPods),
        oldSection
            .flatMap(CustomPersistentVolumeUtil::getWalPath),
        List.of(),
        fieldPrefix);
  }

  @Override
  protected void failValidation(String message, String... fields) throws ValidationFailed {
    failWithFields(message, fields);
  }

}
