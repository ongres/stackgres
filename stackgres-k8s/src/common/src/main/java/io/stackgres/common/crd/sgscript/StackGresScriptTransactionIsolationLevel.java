/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

import java.sql.Connection;

import javax.annotation.Nonnull;

public enum StackGresScriptTransactionIsolationLevel {

  READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED, "READ COMMITTED", "read-committed"),
  REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ, "REPEATABLE READ", "repeatable-read"),
  SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE, "SERIALIZABLE", "serializable");

  private final int jdbcConstant;
  private final @Nonnull String sqlString;
  private final @Nonnull String type;

  StackGresScriptTransactionIsolationLevel(int jdbcConstant, @Nonnull String sqlString,
      @Nonnull String type) {
    this.jdbcConstant = jdbcConstant;
    this.sqlString = sqlString;
    this.type = type;
  }

  public int toJdbcConstant() {
    return jdbcConstant;
  }

  public @Nonnull String toSqlString() {
    return sqlString;
  }

  @Override
  public @Nonnull String toString() {
    return type;
  }

  public static StackGresScriptTransactionIsolationLevel fromString(String value) {
    for (StackGresScriptTransactionIsolationLevel role : StackGresScriptTransactionIsolationLevel
        .values()) {
      if (role.toString().equals(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException(value + " can not be converted to a "
        + StackGresScriptTransactionIsolationLevel.class.getName());
  }
}
