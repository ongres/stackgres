/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@SuppressFBWarnings(value = "NM_CONFUSING",
      justification = "Not an issue")
public class ClusterLogEntryDto {

  private String logTime;

  private Integer logTimeIndex;

  private String logType;

  private String podName;

  private String role;

  private String errorLevel;

  private String message;

  private String userName;

  private String databaseName;

  private Integer processId;

  private String connectionFrom;

  private String sessionId;

  private Integer sessionLineNum;

  private String commandTag;

  private String sessionStartTime;

  private String virtualTransactionId;

  private Integer transactionId;

  private String sqlStateCode;

  private String detail;

  private String hint;

  private String internalQuery;

  private Integer internalQueryPos;

  private String context;

  private String query;

  private Integer queryPos;

  private String location;

  private String applicationName;

  public String getLogTime() {
    return logTime;
  }

  public void setLogTime(String logTime) {
    this.logTime = logTime;
  }

  public Integer getLogTimeIndex() {
    return logTimeIndex;
  }

  public void setLogTimeIndex(Integer logTimeIndex) {
    this.logTimeIndex = logTimeIndex;
  }

  public String getLogType() {
    return logType;
  }

  public void setLogType(String logType) {
    this.logType = logType;
  }

  public String getPodName() {
    return podName;
  }

  public void setPodName(String podName) {
    this.podName = podName;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getErrorLevel() {
    return errorLevel;
  }

  public void setErrorLevel(String errorLevel) {
    this.errorLevel = errorLevel;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public Integer getProcessId() {
    return processId;
  }

  public void setProcessId(Integer processId) {
    this.processId = processId;
  }

  public String getConnectionFrom() {
    return connectionFrom;
  }

  public void setConnectionFrom(String connectionFrom) {
    this.connectionFrom = connectionFrom;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public Integer getSessionLineNum() {
    return sessionLineNum;
  }

  public void setSessionLineNum(Integer sessionLineNum) {
    this.sessionLineNum = sessionLineNum;
  }

  public String getCommandTag() {
    return commandTag;
  }

  public void setCommandTag(String commandTag) {
    this.commandTag = commandTag;
  }

  public String getSessionStartTime() {
    return sessionStartTime;
  }

  public void setSessionStartTime(String sessionStartTime) {
    this.sessionStartTime = sessionStartTime;
  }

  public String getVirtualTransactionId() {
    return virtualTransactionId;
  }

  public void setVirtualTransactionId(String virtualTransactionId) {
    this.virtualTransactionId = virtualTransactionId;
  }

  public Integer getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(Integer transactionId) {
    this.transactionId = transactionId;
  }

  public String getSqlStateCode() {
    return sqlStateCode;
  }

  public void setSqlStateCode(String sqlStateCode) {
    this.sqlStateCode = sqlStateCode;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public String getHint() {
    return hint;
  }

  public void setHint(String hint) {
    this.hint = hint;
  }

  public String getInternalQuery() {
    return internalQuery;
  }

  public void setInternalQuery(String internalQuery) {
    this.internalQuery = internalQuery;
  }

  public Integer getInternalQueryPos() {
    return internalQueryPos;
  }

  public void setInternalQueryPos(Integer internalQueryPos) {
    this.internalQueryPos = internalQueryPos;
  }

  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public Integer getQueryPos() {
    return queryPos;
  }

  public void setQueryPos(Integer queryPos) {
    this.queryPos = queryPos;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
