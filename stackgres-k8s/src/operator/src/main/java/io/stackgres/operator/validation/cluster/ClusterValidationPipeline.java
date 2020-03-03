/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class ClusterValidationPipeline implements ValidationPipeline<StackGresClusterReview> {

  private final Validator validator;
  private final Instance<ClusterValidator> validators;

  @Inject
  public ClusterValidationPipeline(Instance<ClusterValidator> validators, Validator validator) {
    this.validators = validators;
    this.validator = validator;
  }

  /**
   * Validate all {@code Validator}s in sequence.
   */
  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();
    if (cluster != null) {
      Set<ConstraintViolation<StackGresCluster>> violations = validator.validate(cluster);

      if (!violations.isEmpty()) {
        throw new ValidationFailed(violations);
      }
    }

    for (ClusterValidator validator : validators) {
      validator.validate(review);
    }
  }

}
