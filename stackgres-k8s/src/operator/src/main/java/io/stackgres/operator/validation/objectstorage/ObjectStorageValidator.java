/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.objectstorage;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public interface ObjectStorageValidator extends Validator<ObjectStorageReview> {

  default void fail(String reason, String message) throws ValidationFailed {
    fail(HasMetadata.getKind(StackGresObjectStorage.class), reason, message);
  }
}
