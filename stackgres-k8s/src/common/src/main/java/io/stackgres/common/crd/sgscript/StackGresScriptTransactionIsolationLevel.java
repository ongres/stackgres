/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

import java.sql.Connection;

import org.jetbrains.annotations.NotNull;

public enum StackGresScriptTransactionIsolationLevel {

  READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED, "READ COMMITTED", "read-committed"),
  REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ, "REPEATABLE READ", "repeatable-read"),
  SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE, "SERIALIZABLE", "serializable");

  private final int jdbcConstant;
  private final @NotNull String sqlString;
  private final @NotNull String type;

  StackGresScriptTransactionIsolationLevel(int jdbcConstant, @NotNull String sqlString,
      @NotNull String type) {
    this.jdbcConstant = jdbcConstant;
    this.sqlString = sqlString;
    this.type = type;
  }

  public @NotNull int toJdbcConstant() {
    return jdbcConstant;
  }

  public @NotNull String toSqlString() {
    return sqlString;
  }

  @Override
  public @NotNull String toString() {
    return type;
  }

  public static StackGresScriptTransactionIsolationLevel fromString(String from) {
    for (StackGresScriptTransactionIsolationLevel value : StackGresScriptTransactionIsolationLevel
        .values()) {
      if (value.toString().equals(from)) {
        return value;
      }
    }
    throw new IllegalArgumentException(from + " can not be converted to a "
        + StackGresScriptTransactionIsolationLevel.class.getName());
  }
}
