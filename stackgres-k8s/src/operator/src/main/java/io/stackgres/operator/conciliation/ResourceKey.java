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
      HasMetadata resource1,
      HasMetadata resource2) {
    return ResourceKey.create(generator, resource1).equals(ResourceKey.create(generator, resource2));
  }

}
