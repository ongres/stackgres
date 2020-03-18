/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

public interface DefaultCustomResourceFactory<T> {

  String DEFAULT_RESOURCE_NAME_PREFIX = "generated-from-default-";

  T buildResource();

  default String generateDefaultName() {
    long epoch = System.currentTimeMillis();
    return DEFAULT_RESOURCE_NAME_PREFIX + epoch;
  }

}
