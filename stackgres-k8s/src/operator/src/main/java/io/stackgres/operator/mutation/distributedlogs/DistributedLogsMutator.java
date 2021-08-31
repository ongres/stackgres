/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface DistributedLogsMutator extends JsonPatchMutator<StackGresDistributedLogsReview> {

  JsonPointer CLUSTER_CONFIG_POINTER = JsonPointer.of("spec");

  default JsonPointer getTargetPointer(String field) throws NoSuchFieldException {
    String jsonField = getJsonMappingField(field, StackGresDistributedLogsSpec.class);
    return CLUSTER_CONFIG_POINTER.append(jsonField);
  }

  static String getJsonMappingField(String field, Class<?> clazz) throws NoSuchFieldException {
    return clazz.getDeclaredField(field)
        .getAnnotation(JsonProperty.class)
        .value();
  }
}
