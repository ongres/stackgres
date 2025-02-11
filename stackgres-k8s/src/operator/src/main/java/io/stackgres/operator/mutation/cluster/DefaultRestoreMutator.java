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
import io.stackgres.operator.initialization.DefaultLoaderFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultRestoreMutator implements ClusterMutator {

  private final DefaultLoaderFactory<StackGresClusterRestore> defaultRestoreFactory;

  private final ObjectMapper jsonMapper;

  @Inject
  public DefaultRestoreMutator(
      DefaultLoaderFactory<StackGresClusterRestore> defaultRestoreFactory,
      ObjectMapper jsonMapper) {
    this.defaultRestoreFactory = defaultRestoreFactory;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() == Operation.CREATE
        && resource.getSpec().getInitialData() != null
        && resource.getSpec().getInitialData().getRestore() != null) {
      try {
        resource.getSpec().getInitialData().setRestore(
            jsonMapper.readerForUpdating(defaultRestoreFactory.buildResource()).readValue(
                jsonMapper.valueToTree(
                    resource.getSpec().getInitialData().getRestore()).toString()));
      } catch (JsonProcessingException ex) {
        throw new RuntimeException(ex);
      }
    }

    return resource;
  }

}
