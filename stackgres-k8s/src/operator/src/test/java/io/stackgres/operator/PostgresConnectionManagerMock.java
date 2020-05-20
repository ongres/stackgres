/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.sql.Connection;
import java.sql.SQLException;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.test.Mock;
import io.stackgres.apiweb.distributedlogs.PostgresConnectionManager;
import org.jooq.lambda.Unchecked;

@Mock
@ApplicationScoped
public class PostgresConnectionManagerMock extends PostgresConnectionManager {

  public Connection getConnection(String host, String username, String password, String database)
      throws SQLException {
    if (host.contains(":")) {
      throw new UnsupportedOperationException();
    }
    String exposedHost = Unchecked.supplier(() -> ItHelper.createExposedHost(
        AbstractStackGresOperatorIt.getContainer(), host, 5432)).get();
    return super.getConnection(exposedHost, username, password, database);
  }

}
