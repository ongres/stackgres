/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public abstract class AbstractValuesMutator<R extends HasMetadata,
    T extends AdmissionReview<R>, S extends HasMetadata>
    implements Mutator<R, T> {

  private final DefaultCustomResourceFactory<R, S> factory;
  private final ObjectMapper jsonMapper;

  protected AbstractValuesMutator(DefaultCustomResourceFactory<R, S> factory,
      ObjectMapper jsonMapper) {
    this.factory = factory;
    this.jsonMapper = jsonMapper;
  }

  public AbstractValuesMutator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.factory = null;
    this.jsonMapper = null;
  }

  @Override
  public R mutate(T review, R resource) {
    if (review.getRequest().getOperation() != Operation.CREATE) {
      return resource;
    }
    try {
      R defaultValue = getDefaultValue(resource);
      return jsonMapper.treeToValue(
          jsonMapper
          .readerForUpdating(jsonMapper.valueToTree(defaultValue))
          .readValue(jsonMapper.valueToTree(resource).toString()),
          getResourceClass());
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

  protected R getDefaultValue(R resource) {
    S sourceResource = createSourceResource(resource);
    R defaultValue = factory.buildResource(sourceResource);
    return defaultValue;
  }

  protected abstract S createSourceResource(R resource);

  protected abstract Class<R> getResourceClass();

}
