/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface DistributedLogsMutator extends JsonPatchMutator<StackGresDistributedLogsReview> {

  default JsonPointer getTargetPointer(String field) {
    String jsonField = getJsonMappingField(field, StackGresDistributedLogsSpec.class);
    return SPEC_POINTER.append(jsonField);
  }

}
