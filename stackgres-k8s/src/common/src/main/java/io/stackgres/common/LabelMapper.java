/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public interface LabelMapper {

  default String appKey() {
    return StackGresContext.APP_KEY;
  }

  String appName();

  String resourceNameKey();

  String resourceNamespaceKey();

  String resourceUidKey();
}
