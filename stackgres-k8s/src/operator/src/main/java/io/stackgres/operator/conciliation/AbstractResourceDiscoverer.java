/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.function.Predicate;

import javax.enterprise.inject.Instance;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

public abstract class AbstractResourceDiscoverer<T extends GenerationContext<?>>
    extends AbstractDiscoverer<ResourceGenerator<T>>
    implements ResourceGenerationDiscoverer<T> {

  final DecoratorDiscoverer<T> decoratorDiscoverer;

  protected AbstractResourceDiscoverer(
      Instance<ResourceGenerator<T>> instance,
      DecoratorDiscoverer<T> decoratorDiscoverer) {
    super(instance);
    this.decoratorDiscoverer = decoratorDiscoverer;
  }

  public AbstractResourceDiscoverer() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.decoratorDiscoverer = null;
  }

  protected boolean isResourceGeneratorSelectedForContext(
      T context, ResourceGenerator<T> resourceGenerator) {
    return true;
  }

  @Override
  public List<HasMetadata> generateResources(T context) {
    ResourceGeneratorFilter resourceGeneratorFilter = createResourceGeneratorFilter(context);
    List<Decorator<T>> decorators = decoratorDiscoverer.discoverDecorator(context);
    return hub.get(context.getVersion())
        .stream()
        .filter(resourceGeneratorFilter)
        .flatMap(resourceGenerator -> resourceGenerator.generateResource(context))
        .map(resource -> decorators.stream().reduce(
            resource,
            (decoratedResource, decorator) -> decorator.decorate(context, decoratedResource),
            (u, v) -> v))
        .toList();
  }

  protected ResourceGeneratorFilter createResourceGeneratorFilter(T context) {
    return new ResourceGeneratorFilter(context);
  }

  protected class ResourceGeneratorFilter implements Predicate<ResourceGenerator<T>> {
    protected final T context;
    protected final Integer major;
    protected final Integer minor;

    protected ResourceGeneratorFilter(T context) {
      this.context = context;
      this.major = context.getKubernetesVersion()
          .map(VersionInfo::getMajor)
          .map(n -> n.replaceAll("[^0-9]", ""))
          .map(Integer::parseInt).orElse(null);
      this.minor = context.getKubernetesVersion()
          .map(VersionInfo::getMinor)
          .map(n -> n.replaceAll("[^0-9]", ""))
          .map(Integer::parseInt).orElse(null);
    }

    @Override
    public boolean test(ResourceGenerator<T> resourceGenerator) {
      return applyKubernetesVersionBinder(resourceGenerator)
          && isResourceGeneratorSelectedForContext(context, resourceGenerator);
    }

    private boolean applyKubernetesVersionBinder(ResourceGenerator<T> generator) {
      if (major == null || minor == null) {
        return true;
      }
      if (generator.getClass().isAnnotationPresent(KubernetesVersionBinder.class)) {
        KubernetesVersionBinder kubernetesVersionBinder =
            generator.getClass().getAnnotation(KubernetesVersionBinder.class);
        if (!kubernetesVersionBindingApplyFromVersion(kubernetesVersionBinder)) {
          return false;
        }
        if (!kubernetesVersionBindingApplyUpToVersion(kubernetesVersionBinder)) {
          return false;
        }
      }
      return true;
    }

    private boolean kubernetesVersionBindingApplyFromVersion(
        KubernetesVersionBinder kubernetesVersionBinder) {
      if (!kubernetesVersionBinder.from().isEmpty()) {
        String[] fromSplit = kubernetesVersionBinder.from().split("\\.");
        int fromMajor = Integer.parseInt(fromSplit[0]);
        int fromMinor = Integer.parseInt(fromSplit[1]);
        if (major < fromMajor) {
          return false;
        }
        if (fromMajor == major && minor < fromMinor) {
          return false;
        }
      }
      return true;
    }

    private boolean kubernetesVersionBindingApplyUpToVersion(
        KubernetesVersionBinder kubernetesVersionBinder) {
      if (!kubernetesVersionBinder.to().isEmpty()) {
        String[] toSplit = kubernetesVersionBinder.to().split("\\.");
        int toMajor = Integer.parseInt(toSplit[0]);
        int toMinor = Integer.parseInt(toSplit[1]);
        if (major > toMajor) {
          return false;
        }
        if (toMajor == major && minor > toMinor) {
          return false;
        }
      }
      return true;
    }
  }
}
