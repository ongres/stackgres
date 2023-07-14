/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.stackgres.common.CdiUtil;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import jakarta.enterprise.inject.Instance;

public abstract class RunningContainerFactoryDiscoverer<T extends ContainerContext>
    extends ContainerFactoryResourceDiscoverer<T, RunningContainer> {

  protected RunningContainerFactoryDiscoverer(Instance<ContainerFactory<T>> instance) {
    super(instance);
  }

  public RunningContainerFactoryDiscoverer() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  @Override
  protected int getOrdinalFromAnnotation(RunningContainer annotation) {
    return annotation.value().ordinal();
  }

  @Override
  protected Class<RunningContainer> getAnnotationClass() {
    return RunningContainer.class;
  }

}
