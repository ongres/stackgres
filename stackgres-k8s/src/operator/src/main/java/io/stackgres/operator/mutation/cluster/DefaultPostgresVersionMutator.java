/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultPostgresVersionMutator implements ClusterMutator {

  protected static final ObjectMapper mapper = new ObjectMapper();

  private JsonPointer postgresVersionPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String postgresVersionJson = ClusterMutator.getJsonMappingField("version",
        StackGresClusterPostgres.class);

    postgresVersionPointer = ClusterMutator.CLUSTER_CONFIG_POINTER.append("postgres")
        .append(postgresVersionJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    final StackGresCluster cluster = review.getRequest().getObject();
    final String postgresVersion = cluster.getSpec()
        .getPostgres().getVersion();

    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      if (postgresVersion != null) {
        final String calculatedPostgresVersion = getPostgresFlavorComponent(cluster).findVersion(
            postgresVersion);

        if (!calculatedPostgresVersion.equals(postgresVersion)) {

          JsonNode target = mapper.valueToTree(calculatedPostgresVersion);
          ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
          operations.add(applyReplaceValue(postgresVersionPointer, target));

          return operations.build();
        }
      } else {
        JsonNode target = mapper.valueToTree(getPostgresFlavorComponent(cluster).findVersion(
            StackGresComponent.LATEST));
        ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
        operations.add(applyAddValue(postgresVersionPointer, target));

        return operations.build();
      }
    }

    return ImmutableList.of();
  }

}
