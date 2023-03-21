/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface ShardedClusterMutator extends JsonPatchMutator<StackGresShardedClusterReview> {

  default JsonPointer getTargetPointer(String field) {
    String jsonField = getJsonMappingField(field, StackGresShardedClusterSpec.class);
    return SPEC_POINTER.append(jsonField);
  }

  default JsonPointer getCoordinatorTargetPointer(String field) {
    String jsonField =
        getJsonMappingField(field, StackGresShardedClusterCoordinator.class);
    return getCoordinatorTargetPointer()
        .append(jsonField);
  }

  default JsonPointer getCoordinatorTargetPointer() {
    return getTargetPointer("coordinator");
  }

  default JsonPointer getShardsTargetPointer(String field) {
    String jsonField =
        getJsonMappingField(field, StackGresShardedClusterShards.class);
    return getShardsTargetPointer()
        .append(jsonField);
  }

  default JsonPointer getShardsTargetPointer() {
    return getTargetPointer("shards");
  }

}
