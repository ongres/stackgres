/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public interface ResourceScanner<T> {

  @Nonnull List<T> findResources();

  @Nonnull List<T> findResourcesInNamespace(String namespace);

  default List<T> findByLabels(Map<String, String> labels) {
    throw new UnsupportedOperationException();
  }

  default List<T> findByLabelsAndNamespace(String namespace, Map<String, String> labels) {
    throw new UnsupportedOperationException();
  }

}
