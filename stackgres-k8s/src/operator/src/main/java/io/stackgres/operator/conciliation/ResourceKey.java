/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Objects;

import io.fabric8.kubernetes.api.model.HasMetadata;

public record ResourceKey(String apiVersion, String kind, String namespace, String name) {

  public static ResourceKey create(HasMetadata resource) {
    return new ResourceKey(
        resource.getApiVersion(),
        resource.getKind(),
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
  }

  public static boolean same(HasMetadata resource1, HasMetadata resource2) {
    return ResourceKey.create(resource1).equals(ResourceKey.create(resource2));
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiVersion, kind, name, namespace);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ResourceKey)) {
      return false;
    }
    ResourceKey other = (ResourceKey) obj;
    return Objects.equals(apiVersion, other.apiVersion) && Objects.equals(kind, other.kind)
        && Objects.equals(name, other.name) && Objects.equals(namespace, other.namespace);
  }

}
