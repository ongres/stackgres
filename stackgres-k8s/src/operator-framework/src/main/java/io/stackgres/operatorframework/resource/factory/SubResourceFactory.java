/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.factory;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.KubernetesResource;

import org.jooq.lambda.Seq;

@FunctionalInterface
public interface SubResourceFactory<T extends KubernetesResource, C>
    extends SubResourceStreamFactory<T, C> {

  T createResource(C context);

  @Override
  default Stream<T> streamResources(C context) {
    return Seq.of(createResource(context));
  }

}
