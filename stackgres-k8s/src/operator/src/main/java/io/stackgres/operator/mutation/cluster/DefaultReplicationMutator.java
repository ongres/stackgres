/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresMainReplicationRole;
import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DefaultReplicationMutator implements ClusterMutator {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  private JsonPointer replicationPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String replicationJson = ClusterMutator.getJsonMappingField("replication",
        StackGresClusterSpec.class);

    replicationPointer = ClusterMutator.CLUSTER_CONFIG_POINTER
        .append(replicationJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final StackGresClusterReplication replication =
          Optional.ofNullable(review.getRequest().getObject().getSpec().getReplication())
          .orElseGet(() -> new StackGresClusterReplication());

      if (replication.getMode() == null) {
        replication.setMode(StackGresReplicationMode.ASYNC.toString());
      }
      if (replication.getRole() == null) {
        replication.setRole(StackGresMainReplicationRole.HA_READ.toString());
      }
      if (replication.getSyncInstances() == null && replication.isSynchronousMode()) {
        replication.setSyncInstances(1);
      }
      if (replication.getGroups() != null) {
        Seq.seq(replication.getGroups()).zipWithIndex().forEach(group -> {
          if (group.v1.getName() == null) {
            group.v1.setName("group-" + (group.v2 + 1));
          }
          if (group.v1.getRole() == null) {
            group.v1.setRole(StackGresMainReplicationRole.HA_READ.toString());
          }
        });
      }

      JsonNode target = JSON_MAPPER.valueToTree(replication);
      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      if (review.getRequest().getObject().getSpec().getReplication() != null) {
        operations.add(applyReplaceValue(replicationPointer, target));
      } else {
        operations.add(applyAddValue(replicationPointer, target));
      }

      return operations.build();
    }

    return ImmutableList.of();
  }

}
