/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.inject.Singleton;

@Singleton
public class PostgresConnectionManager {

  public Connection getConnection(String host, String username, String password, String database)
      throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("user", username);
    properties.setProperty("password", password);
    return DriverManager.getConnection("jdbc:postgresql://" + host + "/" + database, properties);
  }

}
