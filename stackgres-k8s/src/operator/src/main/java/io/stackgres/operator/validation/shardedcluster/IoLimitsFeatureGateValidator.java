/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.CustomPersistentVolumeUtil;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsCustomPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsPersistentVolumeIoLimits;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorkers;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.IoLimitsFeatureGate;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class IoLimitsFeatureGateValidator implements ShardedClusterValidator {

  private final IoLimitsFeatureGate ioLimitsFeatureGate;

  @Inject
  public IoLimitsFeatureGateValidator(IoLimitsFeatureGate ioLimitsFeatureGate) {
    this.ioLimitsFeatureGate = ioLimitsFeatureGate;
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    StackGresShardedClusterSpec spec = Optional.of(review.getRequest())
        .map(AdmissionRequest::getObject)
        .map(StackGresShardedCluster::getSpec)
        .orElse(null);
    if (spec == null) {
      return;
    }
    boolean hasIoLimits = hasIoLimits(spec.getCoordinator())
        || hasIoLimits(spec.getShards())
        || hasIoLimitsInOverrides(spec.getShards());
    if (hasIoLimits && !ioLimitsFeatureGate.isEnabled()) {
      failWithFields("To enable per-volume I/O limits you must add the \"io-limits\""
          + " feature gate under \".spec.featureGates\" of the SGConfig. This can only be done"
          + " by the operator administrator. Be aware that setting I/O limits requires the Pods"
          + " to run a privileged init container as root and to mount the host cgroup filesystem.",
          ".spec.coordinator.pods.persistentVolume.ioLimits",
          ".spec.shards.pods.persistentVolume.ioLimits");
    }
  }

  private static boolean hasIoLimitsInOverrides(StackGresShardedClusterWorkers shards) {
    return Optional.ofNullable(shards)
        .map(StackGresShardedClusterWorkers::getOverrides)
        .stream()
        .flatMap(List::stream)
        .map(StackGresShardedClusterWorker::getPodsForWorkers)
        .anyMatch(IoLimitsFeatureGateValidator::hasIoLimits);
  }

  private static boolean hasIoLimits(StackGresClusterSpec spec) {
    return Optional.ofNullable(spec)
        .map(StackGresClusterSpec::getPods)
        .map(IoLimitsFeatureGateValidator::hasIoLimits)
        .orElse(false);
  }

  private static boolean hasIoLimits(StackGresClusterPods pods) {
    return Optional.ofNullable(pods)
        .map(StackGresClusterPods::getPersistentVolume)
        .map(StackGresClusterPodsPersistentVolume::getIoLimits)
        .map(IoLimitsFeatureGateValidator::hasIoLimits)
        .orElse(false)
        || CustomPersistentVolumeUtil.getCustomPersistentVolumes(pods)
            .stream()
            .map(StackGresClusterPodsCustomPersistentVolume::getIoLimits)
            .anyMatch(CustomPersistentVolumeUtil::hasIoLimits);
  }

  private static boolean hasIoLimits(StackGresClusterPodsPersistentVolumeIoLimits ioLimits) {
    return ioLimits.getReadIops() != null
        || ioLimits.getWriteIops() != null
        || ioLimits.getReadMiBps() != null
        || ioLimits.getWriteMiBps() != null;
  }

}
