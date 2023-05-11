/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresFeatureGates;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class BabelfishFlavorValidator implements ClusterValidator {

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    boolean hasBabelfishFlavor = Optional.of(review.getRequest())
        .map(AdmissionRequest::getObject)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getFlavor)
        .map(flavor -> flavor.equals(StackGresPostgresFlavor.BABELFISH.toString()))
        .orElse(false);
    boolean hasBabelfishFlavorFeatureGateEnabled = Optional.of(review.getRequest())
        .map(AdmissionRequest::getObject)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getEnabledFeatureGates)
        .map(featureGates -> featureGates.contains(
            StackGresFeatureGates.BABELFISH_FLAVOR.toString()))
        .orElse(false);
    if (hasBabelfishFlavor && !hasBabelfishFlavorFeatureGateEnabled) {
      failWithFields("To enable \"babelfish\" flavor you must add \"babelfish-flavor\""
          + " feature gate under \".spec.nonProductionOptions.enabledFeatureGates\"",
          ".spec.nonProductionOptions.enabledFeatureGates");
    }
    if (hasBabelfishFlavor && review.getRequest().getObject().getSpec().getInstances() > 1) {
      failWithFields("Currently \"babelfish\" flavor only support 1 instance."
          + " Please set \".spec.instances\" to 1", ".spec.instances");
    }
  }

}
