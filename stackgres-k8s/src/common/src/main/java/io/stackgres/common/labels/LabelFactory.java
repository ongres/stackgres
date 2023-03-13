/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import io.fabric8.kubernetes.client.CustomResource;
import org.jetbrains.annotations.NotNull;

public interface LabelFactory<T extends CustomResource<?, ?>> {

  Map<String, String> appLabel();

  Map<String, String> genericLabels(@NotNull T resource);

  LabelMapper<T> labelMapper();

  default String resourceName(@NotNull T resource) {
    return resource.getMetadata().getName();
  }

  default String resourceNamespace(@NotNull T resource) {
    return resource.getMetadata().getNamespace();
  }

  default String resourceUid(@NotNull T resource) {
    return resource.getMetadata().getUid();
  }

}
