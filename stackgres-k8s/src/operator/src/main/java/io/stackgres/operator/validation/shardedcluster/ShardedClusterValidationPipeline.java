/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.SimpleValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;

@ApplicationScoped
public class ShardedClusterValidationPipeline
    implements ValidationPipeline<StackGresShardedClusterReview> {

  private SimpleValidationPipeline<StackGresShardedClusterReview,
      ShardedClusterValidator> genericPipeline;

  /**
   * Validate all {@code Validator}s in sequence.
   */
  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    genericPipeline.validate(review);
  }

  @Inject
  public void setValidators(@Any Instance<ShardedClusterValidator> validators) {
    this.genericPipeline = new SimpleValidationPipeline<>(validators);
  }

}
