/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import org.jetbrains.annotations.NotNull;

public interface StackGresDistributedLogsUtil {

  String POSTGRESQL_VERSION = "12";

  static String getPostgresVersion(StackGresDistributedLogs distributedLogs) {
    return StackGresComponent.POSTGRESQL.get(distributedLogs)
        .getVersion(POSTGRESQL_VERSION);
  }

  static String getPostgresMajorVersion(StackGresDistributedLogs distributedLogs) {
    return StackGresComponent.POSTGRESQL.get(distributedLogs)
        .getMajorVersion(POSTGRESQL_VERSION);
  }

  static String getPostgresBuildMajorVersion(StackGresDistributedLogs distributedLogs) {
    return StackGresComponent.POSTGRESQL.get(distributedLogs)
        .getBuildMajorVersion(POSTGRESQL_VERSION);
  }

  static @NotNull StackGresComponent getPostgresFlavorComponent(
      StackGresDistributedLogs distribtuedLogs) {
    return StackGresComponent.POSTGRESQL;
  }

}
