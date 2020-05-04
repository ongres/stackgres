/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.distributedlogs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;

import org.postgresql.PGProperty;

@ApplicationScoped
public class PostgresConnectionManager {

  public Connection getConnection(String host, String username, String password, String database)
      throws SQLException {
    Properties properties = new Properties();
    PGProperty.USER.set(properties, username);
    PGProperty.PASSWORD.set(properties, password);
    return DriverManager.getConnection("jdbc:postgresql://" + host + "/" + database, properties);
  }

}
