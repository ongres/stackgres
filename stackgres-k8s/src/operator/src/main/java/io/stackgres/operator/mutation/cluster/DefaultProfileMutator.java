/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;

@ApplicationScoped
public class DefaultProfileMutator extends AbstractDefaultResourceMutator<StackGresProfile>
    implements ClusterMutator {

  @Override
  protected String getTargetPropertyValue(StackGresCluster targetCluster) {
    return targetCluster.getSpec().getResourceProfile();
  }

  @Override
  protected JsonPointer getTargetPointer() throws NoSuchFieldException {
    String jsonField = StackGresClusterSpec.class
        .getDeclaredField("resourceProfile")
        .getAnnotation(JsonProperty.class)
        .value();
    return CLUSTER_CONFIG_POINTER.append(jsonField);

  }
}
