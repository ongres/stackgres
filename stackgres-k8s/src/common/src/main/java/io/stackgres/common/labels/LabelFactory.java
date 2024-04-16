/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import java.util.Map;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.client.CustomResource;

public interface LabelFactory<T extends CustomResource<?, ?>> {

  Map<String, String> appLabel();

  Map<String, String> genericLabels(@Nonnull T resource);

  LabelMapper<T> labelMapper();

  default String resourceName(@Nonnull T resource) {
    return resource.getMetadata().getName();
  }

  default String resourceNamespace(@Nonnull T resource) {
    return resource.getMetadata().getNamespace();
  }

  default String resourceUid(@Nonnull T resource) {
    return resource.getMetadata().getUid();
  }

}
