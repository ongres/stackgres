/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.common;

import java.util.Optional;

public interface ResourceFinder<T> {

  /**
   * Will look for a global resource by it's name.
   * @param name the name of the resource
   * @retun the result of the search
   */
  Optional<T> findByName(String name);

  /**
   * Will look for a resource by it's name and namespace.
   * @param name the name of the resource
   * @param namespace the namespace of the resource
   * @retun the result of the search
   */
  Optional<T> findByNameAndNamespace(String name, String namespace);

}
