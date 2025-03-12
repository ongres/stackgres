/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import org.jetbrains.annotations.NotNull;

public enum StorageEncryptionMethod {

  SODIUM("sodium"),
  OPENPGP("openpgp");

  private final @NotNull String type;

  StorageEncryptionMethod(@NotNull String type) {
    this.type = type;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static @NotNull StorageEncryptionMethod fromString(@NotNull String value) {
    for (StorageEncryptionMethod role : StorageEncryptionMethod.values()) {
      if (role.toString().equals(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Unknown storage encryption method " + value);
  }

}
