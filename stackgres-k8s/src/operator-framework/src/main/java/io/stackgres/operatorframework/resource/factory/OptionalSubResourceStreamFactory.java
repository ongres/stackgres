/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.factory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@FunctionalInterface
public interface OptionalSubResourceStreamFactory<T extends KubernetesResource, C>
    extends SubResourceStreamFactory<T, C> {

  Stream<Optional<T>> streamOptionalResources(C context);

  default List<T> listOptionalResources(C context) {
    return streamOptionalResources(context)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  default Stream<T> streamResources(C context) {
    return streamOptionalResources(context)
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

}
