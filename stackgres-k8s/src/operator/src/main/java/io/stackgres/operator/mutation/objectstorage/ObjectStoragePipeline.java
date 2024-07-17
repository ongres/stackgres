/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ObjectStoragePipeline
    extends AbstractMutationPipeline<StackGresObjectStorage, StackGresObjectStorageReview> {

  @Inject
  public ObjectStoragePipeline(
      @Any Instance<ObjectStorageMutator> mutators) {
    super(mutators);
  }

}
