/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.controller.ResourceGeneratorContext;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;

public interface StackGresSidecarTransformer<T, C>
    extends StackGresClusterConfigTransformer<C> {

  Container getContainer(ResourceGeneratorContext<C> context);

  default Optional<T> getConfig(StackGresCluster cluster,
      KubernetesClient client) throws Exception {
    return Optional.empty();
  }

  default ImmutableList<Volume> getVolumes(ResourceGeneratorContext<C> context) {
    return ImmutableList.of();
  }

}
