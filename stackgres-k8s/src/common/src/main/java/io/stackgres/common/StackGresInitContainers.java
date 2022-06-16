/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public enum StackGresInitContainers {

  SETUP_ARBITRARY_USER("setup-arbitrary-user"),
  SETUP_DATA_PATHS("setup-data-paths"),
  SETUP_SCRIPTS("setup-scripts"),
  RELOCATE_BINARIES("relocate-binaries"),
  PGBOUNCER_AUTH_FILE("pgbouncer-auth-file"),
  DISTRIBUTEDLOGS_RECONCILIATION_CYCLE("distributedlogs-reconciliation-cycle"),
  CLUSTER_RECONCILIATION_CYCLE("cluster-reconciliation-cycle"),
  MAJOR_VERSION_UPGRADE("major-version-upgrade"),
  RESET_PATRONI("reset-patroni");

  private final String name;

  StackGresInitContainers(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return getName();
  }

}
