/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Optional;

/*
 * Look for resources T in the kubernetes cluster
 */
public interface KubernetesCustomResourceScanner<T> {

  /**
   * scans the cluster for resource <code>T</code> in all namespaces.
   *
   * @return {@code List<T>} if the resource was found or throws an exception.
   */
  List<T> getResources();

  /**
   * scans the cluster for resources T in the given namespaces.
   *
   * @param namespace the namespace to look into
   * @return {@code List<T>} if the resource was found or throws an exception.
   */
  List<T> getResources(String namespace);

  /**
   * scans the cluster for resource <code>T</code> in all namespaces.
   *
   * @return {@code List<T>} if the resource was found or empty otherwise
   */
  Optional<List<T>> findResources();

  /**
   * scans the cluster for resources T in the given namespaces.
   *
   * @param namespace the namespace to look into
   * @return {@code List<T>} if the resource was found or empty otherwise
   */
  Optional<List<T>> findResources(String namespace);

}
