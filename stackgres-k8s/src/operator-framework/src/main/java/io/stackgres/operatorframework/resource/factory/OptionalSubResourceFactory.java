/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.factory;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.KubernetesResource;

@FunctionalInterface
public interface OptionalSubResourceFactory<T extends KubernetesResource, C> {

  Optional<T> createResource(C context);

  default List<T> listResource(C context) {
    return createResource(context)
        .map(ImmutableList::of)
        .orElse(ImmutableList.of());
  }

}
