/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

public record ExtensionTuple(String extensionName, Optional<String> extensionVersion) {

  public ExtensionTuple(String extensionName, String extensionVersion) {
    this(extensionName, Optional.ofNullable(extensionVersion));
  }

  public ExtensionTuple(String extensionName) {
    this(extensionName, Optional.empty());
  }

}
