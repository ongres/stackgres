/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import jakarta.inject.Singleton;

@Singleton
public class PostgresConnectionManager {

  public Connection getConnection(
      @Nonnull String host,
      int port,
      @Nonnull String database,
      @Nonnull String username,
      @Nonnull String password) throws SQLException {
    return getConnection(host, port, database, username, password, null);
  }
      
  public Connection getConnection(
      @Nonnull String host,
      int port,
      @Nonnull String database,
      @Nonnull String username,
      @Nonnull String password,
      @Nullable Map<String, String> extraProperties) throws SQLException {
    Properties properties = new Properties();
    if (extraProperties != null) {
      extraProperties.entrySet().stream()
              .forEach(extraProperty -> properties.put(
                      extraProperty.getKey(), extraProperty.getValue()));
    }
    properties.setProperty("user", username);
    properties.setProperty("password", password);
    return DriverManager.getConnection(
        "jdbc:postgresql://" + host + ":" + port + "/" + database, properties);
  }

  public Connection getUnixConnection(
      @Nonnull String path,
      int port,
      @Nonnull String database,
      @Nonnull String username,
      @Nonnull String password)
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
