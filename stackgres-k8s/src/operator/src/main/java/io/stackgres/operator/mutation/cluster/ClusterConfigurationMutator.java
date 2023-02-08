/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutatorUtil;

public interface ClusterConfigurationMutator extends JsonPatchMutatorUtil {

  default JsonPointer getConfigurationTargetPointer(String field) {
    String jsonField =
        getJsonMappingField(field, StackGresClusterConfiguration.class);
    return getConfigurationTargetPointer().append(jsonField);
  }

  default JsonPointer getConfigurationTargetPointer() {
    String jsonField =
        getJsonMappingField("configuration", StackGresClusterSpec.class);

    return SPEC_POINTER.append(jsonField);
  }

  default List<JsonPatchOperation> ensureConfigurationNode(StackGresClusterReview review) {
    final StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    StackGresClusterConfiguration configuration = spec.getConfiguration();

    if (configuration == null) {
      configuration = new StackGresClusterConfiguration();
      spec.setConfiguration(configuration);
      final JsonPointer confPointer = getConfigurationTargetPointer();
      final AddOperation configurationAdd = new AddOperation(confPointer,
          JsonPatchMutator.FACTORY.objectNode());
      return List.of(configurationAdd);
    }

    return List.of();
  }
}
