/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import java.util.List;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsConfiguration;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface DistributedLogsConfigurationMutator extends DistributedLogsMutator {

  default JsonPointer getConfigurationTargetPointer(String field) {
    String jsonField =
        getJsonMappingField(field, StackGresDistributedLogsConfiguration.class);
    return getConfigurationTargetPointer().append(jsonField);
  }

  default JsonPointer getConfigurationTargetPointer() {
    String jsonField =
        getJsonMappingField("configuration", StackGresDistributedLogsSpec.class);

    return SPEC_POINTER.append(jsonField);
  }

  default List<JsonPatchOperation> ensureConfigurationNode(StackGresDistributedLogsReview review) {
    final StackGresDistributedLogsSpec spec = review.getRequest().getObject().getSpec();
    StackGresDistributedLogsConfiguration configuration = spec.getConfiguration();

    if (configuration == null) {
      configuration = new StackGresDistributedLogsConfiguration();
      spec.setConfiguration(configuration);
      final JsonPointer confPointer = getConfigurationTargetPointer();
      final AddOperation configurationAdd = new AddOperation(confPointer,
          JsonPatchMutator.FACTORY.objectNode());
      return List.of(configurationAdd);
    }

    return List.of();
  }
}
