/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;

import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgcluster.ClusterRestore;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultRestoreMutator implements ClusterMutator {

  protected static final ObjectMapper mapper = new ObjectMapper();

  private JsonPointer restorePointer;
  private JsonNode defaultNode;

  private DefaultCustomResourceFactory<ClusterRestore> defaultRestoreFactory;

  @Inject
  public DefaultRestoreMutator(
      DefaultCustomResourceFactory<ClusterRestore> defaultRestoreFactory) {
    this.defaultRestoreFactory = defaultRestoreFactory;
  }

  @PostConstruct
  public void init() throws NoSuchFieldException {

    restorePointer = getTargetPointer("restore");

    ClusterRestore defaultRestore = defaultRestoreFactory.buildResource();
    defaultNode = mapper.valueToTree(defaultRestore);

  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {

    ClusterRestore restore = review.getRequest().getObject().getSpec().getRestore();
    if (review.getRequest().getOperation() == Operation.CREATE
        && restore != null) {

      JsonNode target = mapper.valueToTree(restore);

      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      operations.addAll(applyDefaults(restorePointer, defaultNode, target));

      return operations.build();

    }

    return ImmutableList.of();
  }

}
