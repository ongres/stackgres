/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.fabric8.kubernetes.client.CustomResource;

public interface LabelMapper<T extends CustomResource<?, ?>> {

  default String appKey() {
    return StackGresContext.APP_KEY;
  }

  String appName();

  String resourceNameKey(T resource);

  String resourceNamespaceKey(T resource);

  String resourceUidKey(T resource);

  default String getKeyPrefix(T resource) {
    return StackGresContext.STACKGRES_KEY_PREFIX;
  }

}
