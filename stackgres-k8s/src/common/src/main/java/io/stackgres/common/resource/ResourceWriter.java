/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.jetbrains.annotations.NotNull;

public interface ResourceWriter<T extends HasMetadata> {

  T create(@NotNull T resource);

  T update(@NotNull T resource);

  void delete(@NotNull T resource);

  default void deleteWithoutCascading(@NotNull T resource) {
    throw new UnsupportedOperationException();
  }

}
