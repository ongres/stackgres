/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.Optional;

import javax.annotation.Nonnull;

public interface CustomResourceFinder<T> {

  /**
   * Will look for a customer resource by it's name in the given namespace.
   * @param name the name of the resource
   * @param namespace the namespace in which the resource should be located
   * @return the result of the search
   */
  @Nonnull Optional<T> findByNameAndNamespace(String name, String namespace);

}
