/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

public interface KubernetesResourceFinder<T> {

  /**
   * Will look for a resource by it's name in all namespaces.
   * @param name the name of the resource
   * @retun the result of the search
   */
  Optional<T> findByName(String name);
}
