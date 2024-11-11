/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

public interface StackGresDistributedLogsUtil {

  String POSTGRESQL_VERSION = "17";
  String TIMESCALEDB_EXTENSION_NAME = "timescaledb";
  String TIMESCALEDB_EXTENSION_VERSION = "2.17.0";

  static String getPostgresVersion(StackGresDistributedLogs distributedLogs) {
    return Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getStatus)
        .map(StackGresDistributedLogsStatus::getPostgresVersion)
        .orElseGet(() -> StackGresComponent.POSTGRESQL.get(distributedLogs)
            .getVersion(POSTGRESQL_VERSION));
  }

  static @NotNull StackGresComponent getPostgresFlavorComponent(
      StackGresDistributedLogs distribtuedLogs) {
    return StackGresComponent.POSTGRESQL;
  }

  static List<ExtensionTuple> getDefaultDistributedLogsExtensions(
      StackGresDistributedLogs distributedLogs) {
    return getDefaultDistributedLogsExtensions(
        getPostgresVersion(distributedLogs),
        StackGresVersion.getStackGresVersion(distributedLogs));
  }

  static List<ExtensionTuple> getDefaultDistributedLogsExtensions(
      String pgVersion, StackGresVersion sgVersion) {
    return Seq.seq(StackGresUtil.getDefaultClusterExtensions(
        sgVersion,
        pgVersion,
        StackGresPostgresFlavor.VANILLA.toString())).append(
            new ExtensionTuple(TIMESCALEDB_EXTENSION_NAME, TIMESCALEDB_EXTENSION_VERSION))
        .toList();
  }

}
