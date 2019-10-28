/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;

/*
 * Look for resources T in the kubernetes cluster
 */
public interface KubernetesScanner<T> {


  /**
   * scans the cluster for resource <code>T</code> in all namespaces.
   *
   * @return T if the resource was found or empty otherwise
   */
  Optional<T> findResources();

  /**
   * scans the cluster for resources T in the given namespaces.
   *
   * @param namespace the namespace to look into
   * @return T if the resource was found or empty otherwise
   */
  Optional<T> findResources(String namespace);

}
