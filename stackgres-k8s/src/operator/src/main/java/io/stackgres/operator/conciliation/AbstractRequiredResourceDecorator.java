/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

public abstract class AbstractRequiredResourceDecorator<T>
    implements RequiredResourceDecorator<T> {

  private final DecoratorDiscoverer<T> decoratorDiscoverer;
  private final ResourceGenerationDiscoverer<T> generatorsDiscoverer;

  protected AbstractRequiredResourceDecorator(
      DecoratorDiscoverer<T> decoratorDiscoverer,
      ResourceGenerationDiscoverer<T> generatorsDiscoverer) {
    this.decoratorDiscoverer = decoratorDiscoverer;
    this.generatorsDiscoverer = generatorsDiscoverer;
  }

  @Override
  public List<HasMetadata> decorateResources(T context) {
    final List<HasMetadata> discoveredResources = getGenerators().getResourceGenerators(context)
        .stream().flatMap(generator -> generator.generateResource(context)).toList();

    List<Decorator<T>> discoveredDecorators =
        getDecoratorDiscoverer().discoverDecorator(context);

    discoveredDecorators.forEach(decorator -> decorator.decorate(context, discoveredResources));
    return discoveredResources;
  }

  public DecoratorDiscoverer<T> getDecoratorDiscoverer() {
    return decoratorDiscoverer;
  }

  public ResourceGenerationDiscoverer<T> getGenerators() {
    return generatorsDiscoverer;
  }

}
