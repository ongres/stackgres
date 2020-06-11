/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.distributedlogs;

public enum PostgresTableFields {

  LOG_TIME(LogTableFields.LOG_TIME),
  LOG_TIME_INDEX(LogTableFields.LOG_TIME_INDEX),
  POD_NAME(LogTableFields.POD_NAME),
  ROLE(LogTableFields.ROLE),
  ERROR_SEVERITY(LogTableFields.ERROR_SEVERITY),
  MESSAGE(LogTableFields.MESSAGE),

  USER_NAME("user_name"),
  DATABASE_NAME("database_name"),
  PROCESS_ID("process_id"),
  CONNECTION_FROM("connection_from"),
  SESSION_ID("session_id"),
  SESSION_LINE_NUM("session_line_num"),
  COMMAND_TAG("command_tag"),
  SESSION_START_TIME("session_start_time"),
  VIRTUAL_TRANSACTION_ID("virtual_transaction_id"),
  TRANSACTION_ID("transaction_id"),
  DETAIL("detail"),
  HINT("hint"),
  INTERNAL_QUERY("internal_query"),
  INTERNAL_QUERY_POS("internal_query_pos"),
  CONTEXT("context"),
  QUERY("query"),
  QUERY_POS("query_pos"),
  LOCATION("location"),
  APPLICATION_NAME("application_name"),
  SQL_STATE_CODE("sql_state_code");

  private final String fieldName;

  PostgresTableFields(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getFieldName() {
    return fieldName;
  }
}
