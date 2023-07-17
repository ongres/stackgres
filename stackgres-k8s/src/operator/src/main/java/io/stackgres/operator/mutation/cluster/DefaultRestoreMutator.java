/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultRestoreMutator implements ClusterMutator {

  private final DefaultCustomResourceFactory<StackGresClusterRestore> defaultRestoreFactory;

  private final ObjectMapper jsonMapper;

  @Inject
  public DefaultRestoreMutator(
      DefaultCustomResourceFactory<StackGresClusterRestore> defaultRestoreFactory,
      ObjectMapper jsonMapper) {
    this.defaultRestoreFactory = defaultRestoreFactory;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() == Operation.CREATE
        && resource.getSpec().getInitData() != null
        && resource.getSpec().getInitData().getRestore() != null) {
      try {
        resource.getSpec().getInitData().setRestore(
            jsonMapper.readerForUpdating(defaultRestoreFactory.buildResource()).readValue(
                jsonMapper.valueToTree(
                    resource.getSpec().getInitData().getRestore()).toString()));
      } catch (JsonProcessingException ex) {
        throw new RuntimeException(ex);
      }
    }

    return resource;
  }

}
