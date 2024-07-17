/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.stream;

import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.INVALID_CR_REFERENCE)
public class ClusterValidator implements StreamValidator {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public ClusterValidator(
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(StackGresStreamReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.CREATE
        && Objects.equals(
            review.getRequest().getObject().getSpec().getSource().getType(),
            StreamSourceType.SGCLUSTER.toString())) {
      String cluster = review.getRequest().getObject().getSpec().getSource().getSgCluster().getName();
      String namespace = review.getRequest().getObject().getMetadata().getNamespace();
      checkIfClusterExists(cluster, namespace,
          "SGCluster " + cluster + " not found");
    }
  }

  private void checkIfClusterExists(String cluster, String namespace,
      String onError) throws ValidationFailed {
    Optional<StackGresCluster> clusterOpt = clusterFinder
        .findByNameAndNamespace(cluster, namespace);

    if (clusterOpt.isEmpty()) {
      fail(onError);
    }
  }

}
