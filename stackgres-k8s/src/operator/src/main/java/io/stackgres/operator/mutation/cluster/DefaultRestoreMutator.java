/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultRestoreMutator implements ClusterMutator {

  private final ObjectMapper jsonMapper;

  private final JsonPointer restorePointer;
  private final JsonNode defaultNode;

  @Inject
  public DefaultRestoreMutator(ObjectMapper jsonMapper,
      DefaultCustomResourceFactory<StackGresClusterRestore> defaultRestoreFactory) {
    this.jsonMapper = jsonMapper;

    String initDataJson = getJsonMappingField("initData",
        StackGresClusterSpec.class);

    String restoreJsonField = getJsonMappingField("restore",
        StackGresClusterInitData.class);

    this.restorePointer = SPEC_POINTER
        .append(initDataJson).append(restoreJsonField);

    StackGresClusterRestore defaultRestore = defaultRestoreFactory.buildResource();
    this.defaultNode = jsonMapper.valueToTree(defaultRestore);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    final StackGresClusterInitData initData = review.getRequest().getObject().getSpec()
        .getInitData();

    if (review.getRequest().getOperation() == Operation.CREATE
        && initData != null) {
      StackGresClusterRestore restore = initData.getRestore();

      if (restore != null) {

        JsonNode target = jsonMapper.valueToTree(restore);
        ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
        operations.addAll(applyDefaults(restorePointer, defaultNode, target));

        return operations.build();
      }
    }

    return List.of();
  }

}
