/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import org.jetbrains.annotations.NotNull;

public interface ResourceWriter<T extends HasMetadata> {

  default T create(@NotNull T resource) {
    return create(resource, false);
  }

  T create(@NotNull T resource, boolean dryRun);

  default T update(@NotNull T resource) {
    return update(resource, false);
  }

  T update(@NotNull T resource, boolean dryRun);

  default T update(T resource, String patch) {
    return update(resource, patch, false);
  }

  T update(@NotNull T resource, @NotNull Consumer<T> setter);

  default T update(T resource, String patch, boolean dryRun) {
    throw new UnsupportedOperationException();
  }

  default void delete(@NotNull T resource) {
    delete(resource, false);
  }

  void delete(@NotNull T resource, boolean dryRun);

  default void deleteWithoutCascading(@NotNull T resource) {
    deleteWithoutCascading(resource, false);
  }

  default void deleteWithoutCascading(@NotNull T resource, boolean dryRun) {
    throw new UnsupportedOperationException();
  }

}
