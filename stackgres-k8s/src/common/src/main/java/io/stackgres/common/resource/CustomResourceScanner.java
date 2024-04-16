/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

/*
 * Look for resources T in the kubernetes cluster
 */
public interface CustomResourceScanner<T> {

  /**
   * scans the cluster for resource <code>T</code> in all namespaces.
   *
   * @return {@code List<T>} if the resource was found or throws an exception.
   */
  @Nonnull List<T> getResources();

  /**
   * scans the cluster for resources T in the given namespace.
   *
   * @param namespace the namespace to look into
   * @return {@code List<T>} if the resource was found or throws an exception.
   */
  @Nonnull List<T> getResources(String namespace);

  /**
   * scans the cluster for resource <code>T</code> in all namespaces for the given labels.
   *
   * @return {@code List<T>} if the resource was found or throws an exception.
   */
  @Nonnull List<T> getResourcesWithLabels(Map<String, String> labels);

  /**
   * scans the cluster for resources T in the given namespace for the given labels.
   *
   * @param namespace the namespace to look into
   * @return {@code List<T>} if the resource was found or throws an exception.
   */
  @Nonnull List<T> getResourcesWithLabels(
      String namespace, Map<String, String> labels);

  /**
   * scans the cluster for resource <code>T</code> in all namespaces.
   *
   * @return {@code List<T>} if the resource was found or empty otherwise
   */
  @Nonnull Optional<List<T>> findResources();

  /**
   * scans the cluster for resources T in the given namespace.
   *
   * @param namespace the namespace to look into
   * @return {@code List<T>} if the resource was found or empty otherwise
   */
  @Nonnull Optional<List<T>> findResources(String namespace);

}
