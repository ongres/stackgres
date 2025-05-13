/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Objects;

import io.fabric8.kubernetes.api.model.HasMetadata;

public record ResourceKey(
    String generatorApiVersion,
    String generatorKind,
    String generatorNamespace,
    String generatorName,
    String apiVersion,
    String kind,
    String namespace,
    String name) {

  public static ResourceKey create(HasMetadata generator, HasMetadata resource) {
    return new ResourceKey(
        generator.getApiVersion(),
        generator.getKind(),
        generator.getMetadata().getNamespace(),
        generator.getMetadata().getName(),
        resource.getApiVersion(),
        resource.getKind(),
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
  }

  public boolean isGeneratedBy(
      HasMetadata generator) {
    return Objects.equals(generator.getApiVersion(), generatorApiVersion)
        && Objects.equals(generator.getKind(), generatorKind)
        && Objects.equals(generator.getMetadata().getNamespace(), generatorNamespace)
        && Objects.equals(generator.getMetadata().getName(), generatorName);
  }

  public static boolean same(
      HasMetadata generator,
      HasMetadata resource,
      DeployedResource deployedResource) {
    if ((generator == null || resource == null)
        && (deployedResource == null || deployedResource.resourceKey() == null)) {
      return true;
    }
    if (generator == null || resource == null) {
      return false;
    }
    if (deployedResource == null || deployedResource.resourceKey() == null) {
      return false;
    }
    var deployedResourceKey = deployedResource.resourceKey();
    return Objects.equals(generator.getApiVersion(), deployedResourceKey.generatorApiVersion)
        && Objects.equals(generator.getKind(), deployedResourceKey.generatorKind)
        && Objects.equals(generator.getMetadata().getNamespace(), deployedResourceKey.generatorNamespace)
        && Objects.equals(generator.getMetadata().getName(), deployedResourceKey.generatorName)
        && Objects.equals(resource.getApiVersion(), deployedResourceKey.apiVersion)
        && Objects.equals(resource.getKind(), deployedResourceKey.kind)
        && Objects.equals(resource.getMetadata().getNamespace(), deployedResourceKey.namespace)
        && Objects.equals(resource.getMetadata().getName(), deployedResourceKey.name);
  }

  public static boolean same(
      HasMetadata resource,
      HasMetadata deployed) {
    if (resource == null && deployed == null) {
      return true;
    }
    if (resource == null) {
      return false;
    }
    if (deployed == null) {
      return false;
    }
    return Objects.equals(resource.getApiVersion(), deployed.getApiVersion())
        && Objects.equals(resource.getKind(), deployed.getKind())
        && Objects.equals(resource.getMetadata().getNamespace(), deployed.getMetadata().getNamespace())
        && Objects.equals(resource.getMetadata().getName(), deployed.getMetadata().getName());
  }

}
