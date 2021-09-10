/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ClusterContext;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

public abstract class AbstractDecoratorResource<T extends ClusterContext>
    implements DecorateResource<T> {

  private DecoratorDiscoverer<T> decoratorDiscoverer;
  private ResourceGenerationDiscoverer<T> generators;

  public AbstractDecoratorResource(DecoratorDiscoverer<T> decoratorDiscoverer,
      ResourceGenerationDiscoverer<T> generators) {
    this.decoratorDiscoverer = decoratorDiscoverer;
    this.generators = generators;
  }

  @Override
  public List<HasMetadata> decorateResources(T context) {
    final List<HasMetadata> resources = getGenerators().getResourceGenerators(context)
        .stream().flatMap(generator -> generator.generateResource(context))
        .collect(Collectors.toUnmodifiableList());

    List<Decorator<T>> decorators =
        getDecoratorDiscoverer().discoverDecorator(context);

    decorators.forEach(decorator -> decorator.decorate(context, resources));
    return resources;
  }

  public DecoratorDiscoverer<T> getDecoratorDiscoverer() {
    return decoratorDiscoverer;
  }

  public ResourceGenerationDiscoverer<T> getGenerators() {
    return generators;
  }

}
