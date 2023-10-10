/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Consumer;

import io.fabric8.kubernetes.client.CustomResource;
import org.jetbrains.annotations.NotNull;

public interface CustomResourceScheduler<T extends CustomResource<?, ?>> {

  default T create(@NotNull T resource) {
    return create(resource, false);
  }

  T create(@NotNull T resource, boolean dryRun);

  default T update(@NotNull T resource) {
    return update(resource, false);
  }

  T update(@NotNull T resource, boolean dryRun);

  T update(@NotNull T resource, @NotNull Consumer<T> setter);

  <S> T updateStatus(@NotNull T resource, @NotNull Consumer<T> setter);

  default void delete(@NotNull T resource) {
    delete(resource, false);
  }

  void delete(@NotNull T resource, boolean dryRun);

}
