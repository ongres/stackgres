/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.customresource.sgcluster.StackgresClusterConfiguration;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

public interface ClusterConfigurationMutator {

  static JsonPointer getTargetPointer(String field) throws NoSuchFieldException {
    String jsonField = ClusterMutator
        .getJsonMappingField(field, StackgresClusterConfiguration.class);
    return getTargetPointer().append(jsonField);
  }

  static JsonPointer getTargetPointer() throws NoSuchFieldException {

    String jsonField = ClusterMutator
        .getJsonMappingField("configuration", StackGresClusterSpec.class);

    return ClusterMutator.CLUSTER_CONFIG_POINTER.append(jsonField);
  }

  static List<JsonPatchOperation> ensureConfigurationNode(StackGresClusterReview review) {

    final StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    StackgresClusterConfiguration configuration = spec.getConfiguration();

    if (configuration == null) {

      try {

        configuration = new StackgresClusterConfiguration();
        spec.setConfiguration(configuration);
        final JsonPointer confPointer = getTargetPointer();
        final AddOperation configurationAdd = new AddOperation(confPointer,
            JsonPatchMutator.FACTORY.objectNode());
        return ImmutableList.of(configurationAdd);

      } catch (NoSuchFieldException e) {
        return ImmutableList.of();
      }
    }

    return ImmutableList.of();

  }
}
