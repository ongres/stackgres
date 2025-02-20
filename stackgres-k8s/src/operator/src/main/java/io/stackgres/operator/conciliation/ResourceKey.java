/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

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

}
