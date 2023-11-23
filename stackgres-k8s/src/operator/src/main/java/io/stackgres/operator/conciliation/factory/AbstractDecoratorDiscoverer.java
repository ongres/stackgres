/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;
import java.util.function.Predicate;

import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.conciliation.AbstractDiscoverer;
import io.stackgres.operator.conciliation.GenerationContext;
import io.stackgres.operator.conciliation.KubernetesVersionBinder;
import jakarta.enterprise.inject.Instance;

public abstract class AbstractDecoratorDiscoverer<T extends GenerationContext<?>>
    extends AbstractDiscoverer<Decorator<T>>
    implements DecoratorDiscoverer<T> {

  protected AbstractDecoratorDiscoverer(Instance<Decorator<T>> instance) {
    super(instance);
  }

  public AbstractDecoratorDiscoverer() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  protected boolean isDecoratorSelectedForContext(
      T context, Decorator<T> resourceGenerator) {
    return true;
  }

  @Override
  public List<Decorator<T>> discoverDecorator(T context) {
    DecoratorFilter resourceGeneratorFilter = createDecoratorFilter(context);
    return hub.get(context.getVersion())
        .stream()
        .filter(resourceGeneratorFilter)
        .toList();
  }

  protected DecoratorFilter createDecoratorFilter(T context) {
    return new DecoratorFilter(context);
  }

  protected class DecoratorFilter implements Predicate<Decorator<T>> {
    protected final T context;
    protected final Integer major;
    protected final Integer minor;

    protected DecoratorFilter(T context) {
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
    public boolean test(Decorator<T> resourceGenerator) {
      return applyKubernetesVersionBinder(resourceGenerator)
          && isDecoratorSelectedForContext(context, resourceGenerator);
    }

    private boolean applyKubernetesVersionBinder(Decorator<T> generator) {
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
