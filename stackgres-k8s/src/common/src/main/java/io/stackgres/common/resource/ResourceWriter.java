/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceWriter<T extends HasMetadata> {

  default T create(@Nonnull T resource) {
    return create(resource, false);
  }

  T create(@Nonnull T resource, boolean dryRun);

  default T update(@Nonnull T resource) {
    return update(resource, false);
  }

  T update(@Nonnull T resource, boolean dryRun);

  default T update(T resource, String patch) {
    return update(resource, patch, false);
  }

  T update(@Nonnull T resource, @Nonnull Consumer<T> setter);

  default T update(T resource, String patch, boolean dryRun) {
    throw new UnsupportedOperationException();
  }

  default void delete(@Nonnull T resource) {
    delete(resource, false);
  }

  void delete(@Nonnull T resource, boolean dryRun);

  default void deleteWithoutCascading(@Nonnull T resource) {
    deleteWithoutCascading(resource, false);
  }

  default void deleteWithoutCascading(@Nonnull T resource, boolean dryRun) {
    throw new UnsupportedOperationException();
  }

}
