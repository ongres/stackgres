/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import io.stackgres.common.CustomPersistentVolumeUtil;
import io.stackgres.common.ErrorType;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.IoLimitsFeatureGate;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class IoLimitsFeatureGateValidator implements ClusterValidator {

  private final IoLimitsFeatureGate ioLimitsFeatureGate;

  @Inject
  public IoLimitsFeatureGateValidator(IoLimitsFeatureGate ioLimitsFeatureGate) {
    this.ioLimitsFeatureGate = ioLimitsFeatureGate;
  }

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    boolean hasIoLimits = Optional.of(review.getRequest())
        .map(AdmissionRequest::getObject)
        .map(CustomPersistentVolumeUtil::hasAnyIoLimits)
        .orElse(false);
    if (hasIoLimits && !ioLimitsFeatureGate.isEnabled()) {
      failWithFields("To enable per-volume I/O limits you must add the \"io-limits\""
          + " feature gate under \".spec.featureGates\" of the SGConfig. This can only be done"
          + " by the operator administrator. Be aware that setting I/O limits requires the Pods"
          + " to run a privileged init container as root and to mount the host cgroup filesystem.",
          ".spec.pods.persistentVolume.ioLimits",
          ".spec.pods.customPersistentVolumes");
    }
  }

}
