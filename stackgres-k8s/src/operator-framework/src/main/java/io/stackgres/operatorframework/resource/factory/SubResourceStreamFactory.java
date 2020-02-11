/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.factory;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.KubernetesResource;

@FunctionalInterface
public interface SubResourceStreamFactory<T extends KubernetesResource, C> {

  Stream<T> create(C context);

  default List<T> list(C context) {
    return create(context)
        .collect(ImmutableList.toImmutableList());
  }

}
