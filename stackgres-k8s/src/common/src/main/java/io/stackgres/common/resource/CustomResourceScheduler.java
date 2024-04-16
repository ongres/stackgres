/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.client.CustomResource;

public interface CustomResourceScheduler<T extends CustomResource<?, ?>> {

  default T create(@Nonnull T resource) {
    return create(resource, false);
  }

  T create(@Nonnull T resource, boolean dryRun);

  default T update(@Nonnull T resource) {
    return update(resource, false);
  }

  T update(@Nonnull T resource, boolean dryRun);

  T update(@Nonnull T resource, @Nonnull Consumer<T> setter);

  <S> T updateStatus(@Nonnull T resource, @Nonnull Consumer<T> setter);

  default void delete(@Nonnull T resource) {
    delete(resource, false);
  }

  void delete(@Nonnull T resource, boolean dryRun);

}
