/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public interface StackGresScopedObject extends StackGresNamedObject {

  StackGresGroupKind getKind();

  default boolean isProfileFor(StackGresGroupKind kind) {
    return getKind().equals(kind);
  }

  default String getNameWithPrefix() {
    return getKind().getContainerPrefix() + getName();
  }

}
