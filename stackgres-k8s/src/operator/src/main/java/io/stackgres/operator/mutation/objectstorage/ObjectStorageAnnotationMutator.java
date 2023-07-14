/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ObjectStorageAnnotationMutator
    extends AbstractAnnotationMutator<StackGresObjectStorage, ObjectStorageReview>
    implements ObjectStorageMutator {
}
