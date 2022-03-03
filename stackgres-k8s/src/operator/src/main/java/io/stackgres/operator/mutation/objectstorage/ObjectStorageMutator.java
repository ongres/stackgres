/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface ObjectStorageMutator extends JsonPatchMutator<ObjectStorageReview> {

  JsonPointer SG_OBJECT_STORAGE_POINTER = JsonPointer.of("spec");
}
