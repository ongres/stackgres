/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Optional;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresFeatureGates;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class BabelfishFlavorValidator implements ShardedClusterValidator {

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    boolean hasBabelfishFlavor = Optional.of(review.getRequest())
        .map(AdmissionRequest::getObject)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getFlavor)
        .map(flavor -> flavor.equals(StackGresPostgresFlavor.BABELFISH.toString()))
        .orElse(false);
    boolean hasBabelfishFlavorFeatureGateEnabled = Optional.of(review.getRequest())
        .map(AdmissionRequest::getObject)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getEnabledFeatureGates)
        .map(featureGates -> featureGates.contains(
            StackGresFeatureGates.BABELFISH_FLAVOR.toString()))
        .orElse(false);
    if (hasBabelfishFlavor && !hasBabelfishFlavorFeatureGateEnabled) {
      failWithFields("To enable \"babelfish\" flavor you must add \"babelfish-flavor\""
          + " feature gate under \".spec.nonProductionOptions.enabledFeatureGates\"",
          ".spec.nonProductionOptions.enabledFeatureGates");
    }
    if (hasBabelfishFlavor
        && (review.getRequest().getObject().getSpec().getCoordinator().getInstances() > 1
            || review.getRequest().getObject().getSpec().getShards().getInstancesPerCluster() > 1
            )) {
      failWithFields("Currently \"babelfish\" flavor only support 1 instance."
          + " Please set \".spec.coordinator.instances\" and \".spec.shards.instancesPerCluster\""
          + " to 1", ".spec.coordinator.instances", ".spec.shards.instancesPerCluster");
    }
  }

}
