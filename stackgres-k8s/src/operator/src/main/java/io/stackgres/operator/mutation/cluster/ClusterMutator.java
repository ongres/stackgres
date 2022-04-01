/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface ClusterMutator extends JsonPatchMutator<StackGresClusterReview> {

  default JsonPointer getTargetPointer(String field) throws NoSuchFieldException {
    String jsonField = getJsonMappingField(field, StackGresClusterSpec.class);
    return SPEC_POINTER.append(jsonField);
  }

}
