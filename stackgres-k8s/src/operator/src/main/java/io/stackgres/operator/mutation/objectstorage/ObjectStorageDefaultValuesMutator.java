/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.objectstorage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.common.StackGresObjectStorageReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractValuesMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ObjectStorageDefaultValuesMutator
    extends AbstractValuesMutator<StackGresObjectStorage, StackGresObjectStorageReview, HasMetadata>
    implements ObjectStorageMutator {

  @Inject
  public ObjectStorageDefaultValuesMutator(
      DefaultCustomResourceFactory<StackGresObjectStorage, HasMetadata> factory,
      ObjectMapper jsonMapper) {
    super(factory, jsonMapper);
  }

  @Override
  protected HasMetadata createSourceResource(StackGresObjectStorage resource) {
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withName(resource.getMetadata().getName())
        .endMetadata()
        .build();
  }

  @Override
  public StackGresObjectStorage mutate(
      StackGresObjectStorageReview review, StackGresObjectStorage resource) {
    if (resource.getSpec().getType() != null
        && !resource.getSpec().getType().equals(getDefaultValue(resource).getSpec().getType())) {
      return resource;
    }
    return super.mutate(review, resource);
  }

  @Override
  protected Class<StackGresObjectStorage> getResourceClass() {
    return StackGresObjectStorage.class;
  }

}
