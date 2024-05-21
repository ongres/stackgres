/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public interface ObjectStorageMutator
    extends Mutator<StackGresObjectStorage, StackGresObjectStorageReview> {

}
