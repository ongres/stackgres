/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.operator.conciliation.KubernetesVersionBinder;
import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerator;

@ApplicationScoped
public class ResourceGenerationDiscovererImpl
    extends ResourceDiscoverer<ResourceGenerator<StackGresClusterContext>>
    implements ResourceGenerationDiscoverer<StackGresClusterContext> {

  @Inject
  public ResourceGenerationDiscovererImpl(
      @Any
          Instance<ResourceGenerator<StackGresClusterContext>> instance) {
    init(instance);
  }

  @Override
  public List<ResourceGenerator<StackGresClusterContext>> getResourceGenerators(
      StackGresClusterContext context) {
    Integer major = context.getKubernetesVersion()
        .map(VersionInfo::getMajor).map(Integer::parseInt).orElse(null);
    Integer minor = context.getKubernetesVersion()
        .map(VersionInfo::getMinor).map(Integer::parseInt).orElse(null);
    return resourceHub.get(context.getVersion()).stream()
        .filter(generator -> applyKubernetesVersionBinder(major, minor, generator))
        .toList();
  }

  private boolean applyKubernetesVersionBinder(Integer major, Integer minor,
      ResourceGenerator<StackGresClusterContext> generator) {
    if (major == null || minor == null) {
      return true;
    }
    if (generator.getClass().isAnnotationPresent(KubernetesVersionBinder.class)) {
      KubernetesVersionBinder kubernetesVersionBinder =
          generator.getClass().getAnnotation(KubernetesVersionBinder.class);
      if (!kubernetesVersionBindingApplyFromVersion(major, minor, kubernetesVersionBinder)) {
        return false;
      }
      if (!kubernetesVersionBindingApplyUpToVersion(major, minor, kubernetesVersionBinder)) {
        return false;
      }
    }
    return true;
  }

  private boolean kubernetesVersionBindingApplyFromVersion(int major, int minor,
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

  private boolean kubernetesVersionBindingApplyUpToVersion(int major, int minor,
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
