/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.BiConsumer;
import java.util.function.Function;

import io.fabric8.kubernetes.client.CustomResource;
import org.jetbrains.annotations.NotNull;

public interface CustomResourceScheduler<T extends CustomResource<?, ?>> {

  T create(@NotNull T resource);

  T update(@NotNull T resource);

  T update(T resource, BiConsumer<T, T> setter);

  <S> T updateStatus(@NotNull T resource, @NotNull Function<T, S> statusGetter,
      @NotNull BiConsumer<T, S> statusSetter);

  void delete(@NotNull T resource);

}
