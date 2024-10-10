/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.util.List;

import io.stackgres.common.component.Component.ComposedComponentVersion;

public interface VersionReader {

  default List<List<Component>> getSubComponents(StackGresContext context, Component component) {
    return component.getSubComponents();
  }

  List<ComposedComponentVersion> getComposedVersions(StackGresContext context, Component component);

  boolean hasImage();

}
