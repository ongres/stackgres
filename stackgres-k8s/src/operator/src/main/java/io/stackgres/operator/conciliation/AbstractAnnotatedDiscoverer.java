/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.lang.annotation.Annotation;

import io.stackgres.common.CdiUtil;
import jakarta.enterprise.inject.Instance;

public abstract class AbstractAnnotatedDiscoverer<
      T, A extends Annotation>
    extends AbstractDiscoverer<T> {

  protected AbstractAnnotatedDiscoverer(Instance<T> instance) {
    super(instance);
  }

  public AbstractAnnotatedDiscoverer() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  @Override
  protected boolean isSelected(T resourceGenerator) {
    return findAnnotation(resourceGenerator, getAnnotationClass()).isPresent();
  }

  protected abstract Class<A> getAnnotationClass();

}
