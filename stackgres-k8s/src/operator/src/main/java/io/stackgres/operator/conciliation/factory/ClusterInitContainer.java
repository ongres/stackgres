/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

public enum ClusterInitContainer {
  NONE,
  USER_SET_UP,
  DATA_PATHS_INITIALIZER,
  INIT_RELOCATE_BINARIES,
  SCRIPTS_SET_UP,
  INIT_PGBOUNCER_AUTH_FILE,
  RECONCILIATION_CYCLE,
  INIT_MAJOR_VERSION_UPGRADE,
  RESET_PATRONI_INIT,
  ;
}
