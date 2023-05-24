/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.Mutator;

public abstract class AbstractValuesMutator<R extends CustomResource<?, ?>,
    T extends AdmissionReview<R>>
    implements Mutator<R, T> {

  private final DefaultCustomResourceFactory<R> factory;
  private final ObjectMapper jsonMapper;

  protected R defaultValue;

  protected AbstractValuesMutator(DefaultCustomResourceFactory<R> factory,
      ObjectMapper jsonMapper) {
    this.factory = factory;
    this.jsonMapper = jsonMapper;
  }

  public AbstractValuesMutator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.factory = null;
    this.jsonMapper = null;
  }

  public void init() {
    this.defaultValue = factory.buildResource();
  }

  @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
      justification = "Wanted behavior")
  @Override
  public R mutate(T review, R resource) {
    if (review.getRequest().getOperation() != Operation.CREATE) {
      return resource;
    }
    try {
      return jsonMapper.treeToValue(
          jsonMapper
          .readerForUpdating(jsonMapper.valueToTree(this.defaultValue))
          .readValue(jsonMapper.valueToTree(resource).toString()),
          getResourceClass());
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

  protected abstract Class<R> getResourceClass();

}
