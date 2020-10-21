/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.sql.SQLException;
import java.util.List;

import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;

public interface DistributedLogsDatabaseReconciliator {

  boolean existsDatabase(StackGresDistributedLogsContext context, String database)
      throws SQLException;

  void createDatabase(StackGresDistributedLogsContext context, String database) throws SQLException;

  void updateRetention(StackGresDistributedLogsContext context, String database, String retention,
      String table) throws SQLException;

  List<String> reconcileRetention(StackGresDistributedLogsContext context, String database,
      String retention, String table) throws SQLException;

}
