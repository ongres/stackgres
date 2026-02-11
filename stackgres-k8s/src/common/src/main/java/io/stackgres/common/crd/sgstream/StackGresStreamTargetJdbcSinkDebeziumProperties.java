/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamTargetJdbcSinkDebeziumProperties {

  private String connectionUrlParameters;

  private Boolean detectInsertMode;

  private Boolean removePlaceholders;

  @JsonProperty("connectionPoolMin_size")
  private Integer connectionPoolMinSize;

  @JsonProperty("connectionPoolMax_size")
  private Integer connectionPoolMaxSize;

  @JsonProperty("connectionPoolAcquire_increment")
  private Integer connectionPoolAcquireIncrement; 

  private Integer connectionPoolTimeout;

  private Boolean connectionRestartOnErrors;

  @JsonProperty("useTimeZone")
  private String useTimeZone;

  @DebeziumDefault("true")
  private Boolean deleteEnabled;

  private Boolean truncateEnabled;

  @DebeziumDefault("upsert")
  private String insertMode;

  @DebeziumDefault("record_key")
  private String primaryKeyMode;

  private List<String> primaryKeyFields;

  @DebeziumDefault("true")
  private Boolean quoteIdentifiers;

  @DebeziumDefault("basic")
  private String schemaEvolution;

  @DebeziumDefault("${original}")
  private String collectionNameFormat;

  private String dialectPostgresPostgisSchema;

  private Boolean dialectSqlserverIdentityInsert;

  private Integer batchSize;

  private Boolean useReductionBuffer;

  private Integer flushMaxRetries;

  private Integer flushRetryDelayMs;

  private String columnNamingStrategy;

  @DebeziumDefault("io.stackgres.stream.jobs.target.migration.StreamMigrationCollectionNamingStrategy")
  private String collectionNamingStrategy;

  @ReferencedField("primaryKeyMode")
  interface PrimaryKeyMode extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "primaryKeyMode kafka is not supported",
      payload = PrimaryKeyMode.class)
  public boolean isTypeMatchSection() {
    return primaryKeyMode == null || !primaryKeyMode.equalsIgnoreCase("kafka");
  }

  public String getConnectionUrlParameters() {
    return connectionUrlParameters;
  }

  public void setConnectionUrlParameters(String connectionUrlParameters) {
    this.connectionUrlParameters = connectionUrlParameters;
  }

  public Boolean getDetectInsertMode() {
    return detectInsertMode;
  }

  public void setDetectInsertMode(Boolean detectInsertMode) {
    this.detectInsertMode = detectInsertMode;
  }

  public Boolean getRemovePlaceholders() {
    return removePlaceholders;
  }

  public void setRemovePlaceholders(Boolean removePlaceholders) {
    this.removePlaceholders = removePlaceholders;
  }

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

  public Boolean getConnectionRestartOnErrors() {
    return connectionRestartOnErrors;
  }

  public void setConnectionRestartOnErrors(Boolean connectionRestartOnErrors) {
    this.connectionRestartOnErrors = connectionRestartOnErrors;
  }

  public String getUseTimeZone() {
    return useTimeZone;
  }

  public void setUseTimeZone(String useTimeZone) {
    this.useTimeZone = useTimeZone;
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

  public String getCollectionNameFormat() {
    return collectionNameFormat;
  }

  public void setCollectionNameFormat(String collectionNameFormat) {
    this.collectionNameFormat = collectionNameFormat;
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

  public Boolean getUseReductionBuffer() {
    return useReductionBuffer;
  }

  public void setUseReductionBuffer(Boolean useReductionBuffer) {
    this.useReductionBuffer = useReductionBuffer;
  }

  public Integer getFlushMaxRetries() {
    return flushMaxRetries;
  }

  public void setFlushMaxRetries(Integer flushMaxRetries) {
    this.flushMaxRetries = flushMaxRetries;
  }

  public Integer getFlushRetryDelayMs() {
    return flushRetryDelayMs;
  }

  public void setFlushRetryDelayMs(Integer flushRetryDelayMs) {
    this.flushRetryDelayMs = flushRetryDelayMs;
  }

  public String getColumnNamingStrategy() {
    return columnNamingStrategy;
  }

  public void setColumnNamingStrategy(String columnNamingStrategy) {
    this.columnNamingStrategy = columnNamingStrategy;
  }

  public String getCollectionNamingStrategy() {
    return collectionNamingStrategy;
  }

  public void setCollectionNamingStrategy(String collectionNamingStrategy) {
    this.collectionNamingStrategy = collectionNamingStrategy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(batchSize, collectionNameFormat, collectionNamingStrategy,
        columnNamingStrategy, connectionPoolAcquireIncrement, connectionPoolMaxSize,
        connectionPoolMinSize, connectionPoolTimeout, connectionRestartOnErrors,
        connectionUrlParameters, deleteEnabled, detectInsertMode, dialectPostgresPostgisSchema,
        dialectSqlserverIdentityInsert, flushMaxRetries, flushRetryDelayMs, insertMode,
        primaryKeyFields, primaryKeyMode, quoteIdentifiers, removePlaceholders, schemaEvolution,
        truncateEnabled, useReductionBuffer, useTimeZone);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamTargetJdbcSinkDebeziumProperties)) {
      return false;
    }
    StackGresStreamTargetJdbcSinkDebeziumProperties other = (StackGresStreamTargetJdbcSinkDebeziumProperties) obj;
    return Objects.equals(batchSize, other.batchSize)
        && Objects.equals(collectionNameFormat, other.collectionNameFormat)
        && Objects.equals(collectionNamingStrategy, other.collectionNamingStrategy)
        && Objects.equals(columnNamingStrategy, other.columnNamingStrategy)
        && Objects.equals(connectionPoolAcquireIncrement, other.connectionPoolAcquireIncrement)
        && Objects.equals(connectionPoolMaxSize, other.connectionPoolMaxSize)
        && Objects.equals(connectionPoolMinSize, other.connectionPoolMinSize)
        && Objects.equals(connectionPoolTimeout, other.connectionPoolTimeout)
        && Objects.equals(connectionRestartOnErrors, other.connectionRestartOnErrors)
        && Objects.equals(connectionUrlParameters, other.connectionUrlParameters)
        && Objects.equals(deleteEnabled, other.deleteEnabled)
        && Objects.equals(detectInsertMode, other.detectInsertMode)
        && Objects.equals(dialectPostgresPostgisSchema, other.dialectPostgresPostgisSchema)
        && Objects.equals(dialectSqlserverIdentityInsert, other.dialectSqlserverIdentityInsert)
        && Objects.equals(flushMaxRetries, other.flushMaxRetries)
        && Objects.equals(flushRetryDelayMs, other.flushRetryDelayMs)
        && Objects.equals(insertMode, other.insertMode)
        && Objects.equals(primaryKeyFields, other.primaryKeyFields)
        && Objects.equals(primaryKeyMode, other.primaryKeyMode)
        && Objects.equals(quoteIdentifiers, other.quoteIdentifiers)
        && Objects.equals(removePlaceholders, other.removePlaceholders)
        && Objects.equals(schemaEvolution, other.schemaEvolution)
        && Objects.equals(truncateEnabled, other.truncateEnabled)
        && Objects.equals(useReductionBuffer, other.useReductionBuffer)
        && Objects.equals(useTimeZone, other.useTimeZone);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
