/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.mutation.AbstractMutationPipeline;

@ApplicationScoped
public class ObjectStoragePipeline
    extends AbstractMutationPipeline<StackGresObjectStorage, ObjectStorageReview> {

  @Inject
  public ObjectStoragePipeline(
      @Any Instance<ObjectStorageMutator> mutators) {
    super(mutators);
  }

}
