/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public interface ResourceFinder<T> {

  /**
   * Will look for a global resource by it's name.
   * @param name the name of the resource
   * @return the result of the search
   */
  @NotNull Optional<T> findByName(@NotNull String name);

  /**
   * Will look for a resource by it's name and namespace.
   * @param name the name of the resource
   * @param namespace the namespace of the resource
   * @return the result of the search
   */
  @NotNull Optional<T> findByNameAndNamespace(@NotNull String name, @NotNull String namespace);

}
