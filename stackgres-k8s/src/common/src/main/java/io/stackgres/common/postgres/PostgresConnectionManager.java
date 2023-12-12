/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;

@Singleton
public class PostgresConnectionManager {

  @SuppressWarnings("unchecked")
  public Connection getConnection(
      @NotNull String host,
      @NotNull int port,
      @NotNull String database,
      @NotNull String username,
      @NotNull String password,
      @SuppressWarnings("uncheked") Map.Entry<String, String>...extraProperties)
          throws SQLException {
    Properties properties = new Properties();
    if (extraProperties != null) {
      Arrays.stream(extraProperties)
              .forEach(extraProperty -> properties.put(
                      extraProperty.getKey(), extraProperty.getValue()));
    }
    properties.setProperty("user", username);
    properties.setProperty("password", password);
    return DriverManager.getConnection(
        "jdbc:postgresql://" + host + ":" + port + "/" + database, properties);
  }

  public Connection getUnixConnection(
      @NotNull String path,
      @NotNull int port,
      @NotNull String database,
      @NotNull String username,
      @NotNull String password)
      throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("user", username);
    properties.setProperty("password", password);
    properties.setProperty("socketFactory", PostgresUnixSocketFactory.class.getName());
    properties.setProperty("socketFactoryArg", path + "/.s.PGSQL." + port);
    return DriverManager.getConnection(
        "jdbc:postgresql://unix/" + database, properties);
  }

}
