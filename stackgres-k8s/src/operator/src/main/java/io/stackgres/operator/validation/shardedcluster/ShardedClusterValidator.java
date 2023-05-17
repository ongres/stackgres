/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface ShardedClusterValidator extends Validator<StackGresShardedClusterReview> {

  default void fail(String reason, String message) throws ValidationFailed {
    fail(HasMetadata.getKind(StackGresShardedCluster.class), reason, message);
  }

  default void fail(String message) throws ValidationFailed {
    ValidationType validationType = this.getClass().getAnnotation(ValidationType.class);
    String errorTypeUri = ErrorType.getErrorTypeUri(validationType.value());
    fail(errorTypeUri, message);
  }

  default void failWithFields(String message, String...fields) throws ValidationFailed {
    ValidationType validationType = this.getClass().getAnnotation(ValidationType.class);
    String errorTypeUri = ErrorType.getErrorTypeUri(validationType.value());
    failWithMessageAndFields(HasMetadata.getKind(StackGresShardedCluster.class),
        errorTypeUri, message, fields);
  }

}
