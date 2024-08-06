/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs;

import java.beans.ConstructorProperties;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterLogEntryDto;

@RegisterForReflection
public class MappedClusterLogEntryDto extends ClusterLogEntryDto {

  @ConstructorProperties({
      "log_time",
      "log_time_index",
      "log_type",
      "pod_name",
      "role",
      "error_severity",
      "message",
      "user_name",
      "database_name",
      "process_id",
      "connection_from",
      "session_id",
      "session_line_num",
      "command_tag",
      "session_start_time",
      "virtual_transaction_id",
      "transaction_id",
      "sql_state_code",
      "detail",
      "hint",
      "internal_query",
      "internal_query_pos",
      "context",
      "query",
      "query_pos",
      "location",
      "application_name"
  })
  public MappedClusterLogEntryDto(String logTime, Integer logTimeIndex, String logType,
      String podName, String role, String errorLevel, String message, String userName,
      String databaseName, Integer processId, String connectionFrom, String sessionId,
      Integer sessionLineNum, String commandTag, String sessionStartTime,
      String virtualTransactionId, Integer transactionId, String sqlStateCode, String detail,
      String hint, String internalQuery, Integer internalQueryPos, String context, String query,
      Integer queryPos, String location, String applicationName) {
    setLogTime(logTime);
    setLogTimeIndex(logTimeIndex);
    setLogType(logType);
    setPodName(podName);
    setRole(role);
    setErrorLevel(errorLevel);
    setMessage(message);
    setUserName(userName);
    setDatabaseName(databaseName);
    setProcessId(processId);
    setConnectionFrom(connectionFrom);
    setSessionId(sessionId);
    setSessionLineNum(sessionLineNum);
    setCommandTag(commandTag);
    setSessionStartTime(sessionStartTime);
    setVirtualTransactionId(virtualTransactionId);
    setTransactionId(transactionId);
    setSqlStateCode(sqlStateCode);
    setDetail(detail);
    setHint(hint);
    setInternalQuery(internalQuery);
    setInternalQueryPos(internalQueryPos);
    setContext(context);
    setQuery(query);
    setQueryPos(queryPos);
    setLocation(location);
    setApplicationName(applicationName);
  }
}
