/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;

public interface StackGresSidecarTransformer<T extends CustomResource>
    extends StackGresClusterConfigTransformer {

  public Container getContainer(StackGresClusterConfig config);

  public default Optional<T> getConfig(StackGresCluster cluster,
      KubernetesClient client) throws Exception {
    return Optional.empty();
  }

  public default ImmutableList<Volume> getVolumes(StackGresClusterConfig config) {
    return ImmutableList.of();
  }

}
