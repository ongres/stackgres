/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultPostgresVersionMutator implements ShardedClusterMutator {

  private ObjectMapper jsonMapper;

  private JsonPointer postgresVersionPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    final String postgresJson = getJsonMappingField("postgres",
        StackGresClusterSpec.class);

    final String postgresVersionJson = getJsonMappingField("version",
        StackGresClusterPostgres.class);
    postgresVersionPointer = SPEC_POINTER.append(postgresJson)
        .append(postgresVersionJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresShardedClusterReview review) {
    final StackGresShardedCluster cluster = review.getRequest().getObject();
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();

    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList
          .builderWithExpectedSize(2);
      if (postgresVersion != null) {
        final String calculatedPostgresVersion = getPostgresFlavorComponent(cluster)
            .get(cluster).getVersion(postgresVersion);

        if (!calculatedPostgresVersion.equals(postgresVersion)) {
          JsonNode target = jsonMapper.valueToTree(calculatedPostgresVersion);
          operations.add(applyReplaceValue(postgresVersionPointer, target));
        }
      } else {
        JsonNode target = jsonMapper.valueToTree(getPostgresFlavorComponent(cluster)
            .get(cluster).getVersion(StackGresComponent.LATEST));
        operations.add(applyAddValue(postgresVersionPointer, target));
      }

      return operations.build();
    }

    return List.of();
  }

  @Inject
  public void setObjectMapper(ObjectMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }
}
