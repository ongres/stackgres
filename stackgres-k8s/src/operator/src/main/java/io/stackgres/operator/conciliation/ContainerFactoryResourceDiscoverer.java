/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.enterprise.inject.Instance;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;

public abstract class ContainerFactoryResourceDiscoverer<
      T extends ContainerContext, A extends Annotation>
    extends AbstractAnnotatedDiscoverer<ContainerFactory<T>, A> {

  @SuppressFBWarnings(value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
      justification = "safe overridable method")
  protected ContainerFactoryResourceDiscoverer(Instance<ContainerFactory<T>> instance) {
    super(instance);
    hub.forEach((key, value) -> {
      value.sort((f1, f2) -> {
        int f1Order = getOrdinalFromAnnotation(getAnnotation(f1, getAnnotationClass()));
        int f2Order = getOrdinalFromAnnotation(getAnnotation(f2, getAnnotationClass()));
        return Integer.compare(f1Order, f2Order);
      });
    });
  }

  public ContainerFactoryResourceDiscoverer() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  protected abstract int getOrdinalFromAnnotation(A annotation);

  @Override
  protected boolean isSelected(ContainerFactory<T> resourceGenerator) {
    return findAnnotation(resourceGenerator, getAnnotationClass()).isPresent();
  }

  public List<ContainerFactory<T>> discoverContainers(T context) {
    return hub.get(context.getGenerationContext().getVersion()).stream()
        .filter(f -> f.isActivated(context))
        .toList();
  }

}
