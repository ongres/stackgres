/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public interface ResourceScanner<T> {

  @NotNull List<T> getResources();

  @NotNull List<T> getResourcesWithLabels(Map<String, String> labels);

  @NotNull List<T> getResourcesInNamespace(String namespace);

  default List<T> getResourcesInNamespaceWithLabels(String namespace, Map<String, String> labels) {
    throw new UnsupportedOperationException();
  }

}
