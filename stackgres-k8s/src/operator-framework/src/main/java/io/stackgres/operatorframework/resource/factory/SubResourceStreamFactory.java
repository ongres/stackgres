/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.factory;

import java.util.List;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.KubernetesResource;

@FunctionalInterface
public interface SubResourceStreamFactory<T extends KubernetesResource, C> {

  Stream<T> streamResources(C context);

  default List<T> listResources(C context) {
    return streamResources(context)
        .toList();
  }

}
