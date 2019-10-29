/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.validation.StackgresClusterReview;
import io.stackgres.operatorframework.ValidationFailed;
import io.stackgres.operatorframework.ValidationPipeline;

@ApplicationScoped
public class ClusterValidationPipeline implements ValidationPipeline<StackgresClusterReview> {

  private Instance<ClusterValidator> validators;

  @Inject
  public ClusterValidationPipeline(Instance<ClusterValidator> validators) {
    this.validators = validators;
  }

  /**
   * Validate all {@code Validator}s in sequence.
   */
  public void validate(StackgresClusterReview admissionReview) throws ValidationFailed {

    for (ClusterValidator validator : validators) {

      validator.validate(admissionReview);

    }

  }

}
