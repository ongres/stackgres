/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operatorframework.JsonPatchMutator;

public interface ClusterMutator extends JsonPatchMutator<StackgresClusterReview> {

  JsonPointer CLUSTER_CONFIG_POINTER = JsonPointer.of("spec");

}
