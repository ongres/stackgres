/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

public interface StackGresPropertyContext<T extends StackGresPropertyReader> {

  default boolean getBoolean(T propertyGetter) {
    return propertyGetter.getBoolean();
  }

  default String getString(T propertyGetter) {
    return propertyGetter.getString();
  }

  default String[] getStringArray(T propertyGetter) {
    return propertyGetter.getStringArray();
  }

  default Optional<String> get(T propertyGetter) {
    return propertyGetter.get();
  }

}
