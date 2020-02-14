/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.factory;

import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.jooq.lambda.Seq;

public interface ContainerResourceFactory<T, C, E>
    extends SubResourceStreamFactory<HasMetadata, C> {

  Container getContainer(C context);

  default ImmutableList<Volume> getVolumes(C context) {
    return ImmutableList.of();
  }

  default Stream<HasMetadata> streamResources(C context) {
    return Seq.empty();
  }

  default Optional<T> getConfig(E config, KubernetesClient client) throws Exception {
    return Optional.empty();
  }

}
