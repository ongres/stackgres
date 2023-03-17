/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.ObjectStorageReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;

@ApplicationScoped
public class ObjectStorageDefaultValuesMutator
    extends AbstractValuesMutator<StackGresObjectStorage, ObjectStorageReview>
    implements ObjectStorageMutator {

  @Inject
  public ObjectStorageDefaultValuesMutator(
      DefaultCustomResourceFactory<StackGresObjectStorage> factory,
      ObjectMapper jsonMapper) {
    super(factory, jsonMapper);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  public StackGresObjectStorage mutate(
      ObjectStorageReview review, StackGresObjectStorage resource) {
    if (resource.getSpec().getType() != null
        && !resource.getSpec().getType().equals(defaultValue.getSpec().getType())) {
      return resource;
    }
    return super.mutate(review, resource);
  }

  @Override
  protected Class<StackGresObjectStorage> getResourceClass() {
    return StackGresObjectStorage.class;
  }

}
