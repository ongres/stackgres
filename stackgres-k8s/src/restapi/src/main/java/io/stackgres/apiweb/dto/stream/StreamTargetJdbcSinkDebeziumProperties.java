/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamTargetJdbcSinkDebeziumProperties {

  @JsonProperty("connectionPoolMin_size")
  private Integer connectionPoolMinSize;

  @JsonProperty("connectionPoolMax_size")
  private Integer connectionPoolMaxSize;

  @JsonProperty("connectionPoolAcquire_increment")
  private Integer connectionPoolAcquireIncrement; 

  private Integer connectionPoolTimeout;

  @JsonProperty("databaseTime_zone")
  private String databaseTimeZone;

  private Boolean deleteEnabled;

  private Boolean truncateEnabled;

  private String insertMode;

  private String primaryKeyMode;

  private List<String> primaryKeyFields;

  private Boolean quoteIdentifiers;

  private String schemaEvolution;

  private String tableNameFormat;

  private String dialectPostgresPostgisSchema;

  private Boolean dialectSqlserverIdentityInsert;

  private Integer batchSize;

  private String columnNamingStrategy;

  private String tableNamingStrategy;

  public Integer getConnectionPoolMinSize() {
    return connectionPoolMinSize;
  }

  public void setConnectionPoolMinSize(Integer connectionPoolMinSize) {
    this.connectionPoolMinSize = connectionPoolMinSize;
  }

  public Integer getConnectionPoolMaxSize() {
    return connectionPoolMaxSize;
  }

  public void setConnectionPoolMaxSize(Integer connectionPoolMaxSize) {
    this.connectionPoolMaxSize = connectionPoolMaxSize;
  }

  public Integer getConnectionPoolAcquireIncrement() {
    return connectionPoolAcquireIncrement;
  }

  public void setConnectionPoolAcquireIncrement(Integer connectionPoolAcquireIncrement) {
    this.connectionPoolAcquireIncrement = connectionPoolAcquireIncrement;
  }

  public Integer getConnectionPoolTimeout() {
    return connectionPoolTimeout;
  }

  public void setConnectionPoolTimeout(Integer connectionPoolTimeout) {
    this.connectionPoolTimeout = connectionPoolTimeout;
  }

  public String getDatabaseTimeZone() {
    return databaseTimeZone;
  }

  public void setDatabaseTimeZone(String databaseTimeZone) {
    this.databaseTimeZone = databaseTimeZone;
  }

  public Boolean getDeleteEnabled() {
    return deleteEnabled;
  }

  public void setDeleteEnabled(Boolean deleteEnabled) {
    this.deleteEnabled = deleteEnabled;
  }

  public Boolean getTruncateEnabled() {
    return truncateEnabled;
  }

  public void setTruncateEnabled(Boolean truncateEnabled) {
    this.truncateEnabled = truncateEnabled;
  }

  public String getInsertMode() {
    return insertMode;
  }

  public void setInsertMode(String insertMode) {
    this.insertMode = insertMode;
  }

  public String getPrimaryKeyMode() {
    return primaryKeyMode;
  }

  public void setPrimaryKeyMode(String primaryKeyMode) {
    this.primaryKeyMode = primaryKeyMode;
  }

  public List<String> getPrimaryKeyFields() {
    return primaryKeyFields;
  }

  public void setPrimaryKeyFields(List<String> primaryKeyFields) {
    this.primaryKeyFields = primaryKeyFields;
  }

  public Boolean getQuoteIdentifiers() {
    return quoteIdentifiers;
  }

  public void setQuoteIdentifiers(Boolean quoteIdentifiers) {
    this.quoteIdentifiers = quoteIdentifiers;
  }

  public String getSchemaEvolution() {
    return schemaEvolution;
  }

  public void setSchemaEvolution(String schemaEvolution) {
    this.schemaEvolution = schemaEvolution;
  }

  public String getTableNameFormat() {
    return tableNameFormat;
  }

  public void setTableNameFormat(String tableNameFormat) {
    this.tableNameFormat = tableNameFormat;
  }

  public String getDialectPostgresPostgisSchema() {
    return dialectPostgresPostgisSchema;
  }

  public void setDialectPostgresPostgisSchema(String dialectPostgresPostgisSchema) {
    this.dialectPostgresPostgisSchema = dialectPostgresPostgisSchema;
  }

  public Boolean getDialectSqlserverIdentityInsert() {
    return dialectSqlserverIdentityInsert;
  }

  public void setDialectSqlserverIdentityInsert(Boolean dialectSqlserverIdentityInsert) {
    this.dialectSqlserverIdentityInsert = dialectSqlserverIdentityInsert;
  }

  public Integer getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(Integer batchSize) {
    this.batchSize = batchSize;
  }

  public String getColumnNamingStrategy() {
    return columnNamingStrategy;
  }

  public void setColumnNamingStrategy(String columnNamingStrategy) {
    this.columnNamingStrategy = columnNamingStrategy;
  }

  public String getTableNamingStrategy() {
    return tableNamingStrategy;
  }

  public void setTableNamingStrategy(String tableNamingStrategy) {
    this.tableNamingStrategy = tableNamingStrategy;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
