/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.customresource.sgcluster.StackgresClusterConfiguration;

public interface ClusterConfigurationMutator {

  JsonPointer CLUSTER_CUSTOM_CONFIGURATION_POINTER = ClusterMutator.CLUSTER_CONFIG_POINTER
      .append("configuration");

  static JsonPointer getTargetPointer(String field) throws NoSuchFieldException {
    String jsonField = ClusterMutator
        .getJsonMappingField(field, StackgresClusterConfiguration.class);
    return CLUSTER_CUSTOM_CONFIGURATION_POINTER.append(jsonField);
  }
}
