/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavor;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Objects;

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
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultPostgresVersionMutator implements ClusterMutator {

  protected static final ObjectMapper mapper = new ObjectMapper();

  private JsonPointer postgresVersionPointer;
  private JsonPointer postgresFlavorPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    final String postgresJson = ClusterMutator.getJsonMappingField("postgres",
        StackGresClusterSpec.class);

    final String postgresVersionJson = ClusterMutator.getJsonMappingField("version",
        StackGresClusterPostgres.class);
    postgresVersionPointer = ClusterMutator.CLUSTER_CONFIG_POINTER.append(postgresJson)
        .append(postgresVersionJson);

    final String postgresFlavorJson = ClusterMutator.getJsonMappingField("flavor",
        StackGresClusterPostgres.class);
    postgresFlavorPointer = ClusterMutator.CLUSTER_CONFIG_POINTER.append(postgresJson)
        .append(postgresFlavorJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    final StackGresCluster cluster = review.getRequest().getObject();
    final String postgresVersion = cluster.getSpec()
        .getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec()
        .getPostgres().getFlavor();

    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList
          .builderWithExpectedSize(2);
      if (postgresVersion != null) {
        final String calculatedPostgresVersion = getPostgresFlavorComponent(postgresFlavor)
            .get(cluster).findVersion(postgresVersion);

        if (!calculatedPostgresVersion.equals(postgresVersion)) {
          JsonNode target = mapper.valueToTree(calculatedPostgresVersion);
          operations.add(applyReplaceValue(postgresVersionPointer, target));
        }
      } else {
        JsonNode target = mapper.valueToTree(getPostgresFlavorComponent(postgresFlavor)
            .get(cluster).findVersion(StackGresComponent.LATEST));
        operations.add(applyAddValue(postgresVersionPointer, target));
      }

      if (!Objects.equals(postgresFlavor, getPostgresFlavor(postgresFlavor))) {
        JsonNode target = mapper.valueToTree(getPostgresFlavor(postgresFlavor));
        operations.add(applyAddValue(postgresFlavorPointer, target));
      }

      return operations.build();
    }

    return ImmutableList.of();
  }

}
