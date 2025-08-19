/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import org.jetbrains.annotations.NotNull;

public enum StackGresReplicationInitializationMode {

  FROM_PRIMARY("FromPrimary"),
  FROM_REPLICA("FromReplica"),
  FROM_EXISTING_BACKUP("FromExistingBackup"),
  FROM_NEWLY_CREATED_BACKUP("FromNewlyCreatedBackup");

  private final @NotNull String mode;

  StackGresReplicationInitializationMode(@NotNull String mode) {
    this.mode = mode;
  }

  public String mode() {
    return mode;
  }

  @Override
  public @NotNull String toString() {
    return mode;
  }

  public static StackGresReplicationInitializationMode fromString(String from) {
    for (StackGresReplicationInitializationMode value : StackGresReplicationInitializationMode.values()) {
      if (value.toString().equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknwon replication initialization mode " + from);
  }

}
