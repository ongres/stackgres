/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

public interface ResourceScanner<T> {

  @NotNull List<T> findResources();

  @NotNull List<T> findResourcesInNamespace(String namespace);

  default List<T> findByLabels(Map<String, String> labels) {
    throw new UnsupportedOperationException();
  }

  default List<T> findByLabelsAndNamespace(String namespace, Map<String, String> labels) {
    throw new UnsupportedOperationException();
  }

}
