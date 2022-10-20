/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.operatorframework.resource.ResourceUtil;

public interface StackGresNamedObject {

  String getName();

  default String getName(String...parameters) {
    return format(getName(), parameters);
  }

  default String format(String format, String...parameters) {
    if (format == null) {
      throw new IllegalArgumentException("Volume " + getClass().getSimpleName()
          + " " + getName() + " has no resource format configured");
    }
    return ResourceUtil.resourceName(String.format(format, (Object[]) parameters));
  }

}
