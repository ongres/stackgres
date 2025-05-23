/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterRestore;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.initialization.DefaultLoaderFactory;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultRestoreMutator implements ShardedClusterMutator {

  private final DefaultLoaderFactory<StackGresShardedClusterRestore> defaultRestoreFactory;

  private final ObjectMapper jsonMapper;

  @Inject
  public DefaultRestoreMutator(
      DefaultLoaderFactory<StackGresShardedClusterRestore> defaultRestoreFactory,
      ObjectMapper jsonMapper) {
    this.defaultRestoreFactory = defaultRestoreFactory;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
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
