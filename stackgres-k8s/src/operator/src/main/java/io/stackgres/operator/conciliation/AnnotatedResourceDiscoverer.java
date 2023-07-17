/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.lang.annotation.Annotation;

import jakarta.enterprise.inject.Instance;

public abstract class AnnotatedResourceDiscoverer<T, A extends Annotation>
    extends ResourceDiscoverer<T> {

  @Override
  protected void init(Instance<T> instance) {
    instance.select(new OperatorVersionBinderLiteral()).stream()
        .filter(f -> findAnnotation(f, getAnnotationClass()).isPresent())
        .forEach(this::appendResourceFactory);
  }

  protected abstract Class<A> getAnnotationClass();

}
