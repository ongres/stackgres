/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import javax.enterprise.inject.Instance;

import io.stackgres.common.CdiUtil;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;

public abstract class InitContainerFactoryDiscoverer<T extends ContainerContext>
    extends ContainerFactoryResourceDiscoverer<T, InitContainer> {

  protected InitContainerFactoryDiscoverer(Instance<ContainerFactory<T>> instance) {
    super(instance);
  }

  public InitContainerFactoryDiscoverer() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  @Override
  protected int getOrdinalFromAnnotation(InitContainer annotation) {
    return annotation.value().ordinal();
  }

  @Override
  protected Class<InitContainer> getAnnotationClass() {
    return InitContainer.class;
  }

}
