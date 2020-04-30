/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
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
  public int hashCode() {
    return Objects.hash(applicationName, commandTag, connectionFrom, context, databaseName, detail,
        errorLevel, hint, internalQuery, internalQueryPos, location, logTime, logTimeIndex, logType,
        message, podName, processId, query, queryPos, role, sessionId, sessionLineNum,
        sessionStartTime, sqlStateCode, transactionId, userName, virtualTransactionId);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterLogEntryDto)) {
      return false;
    }
    ClusterLogEntryDto other = (ClusterLogEntryDto) obj;
    return Objects.equals(applicationName, other.applicationName)
        && Objects.equals(commandTag, other.commandTag)
        && Objects.equals(connectionFrom, other.connectionFrom)
        && Objects.equals(context, other.context)
        && Objects.equals(databaseName, other.databaseName) && Objects.equals(detail, other.detail)
        && Objects.equals(errorLevel, other.errorLevel) && Objects.equals(hint, other.hint)
        && Objects.equals(internalQuery, other.internalQuery)
        && Objects.equals(internalQueryPos, other.internalQueryPos)
        && Objects.equals(location, other.location) && Objects.equals(logTime, other.logTime)
        && Objects.equals(logTimeIndex, other.logTimeIndex)
        && Objects.equals(logType, other.logType) && Objects.equals(message, other.message)
        && Objects.equals(podName, other.podName) && Objects.equals(processId, other.processId)
        && Objects.equals(query, other.query) && Objects.equals(queryPos, other.queryPos)
        && Objects.equals(role, other.role) && Objects.equals(sessionId, other.sessionId)
        && Objects.equals(sessionLineNum, other.sessionLineNum)
        && Objects.equals(sessionStartTime, other.sessionStartTime)
        && Objects.equals(sqlStateCode, other.sqlStateCode)
        && Objects.equals(transactionId, other.transactionId)
        && Objects.equals(userName, other.userName)
        && Objects.equals(virtualTransactionId, other.virtualTransactionId);
  }

}
