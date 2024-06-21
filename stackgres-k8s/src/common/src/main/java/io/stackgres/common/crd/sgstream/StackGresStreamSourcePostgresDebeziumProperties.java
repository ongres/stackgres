/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamSourcePostgresDebeziumProperties {

  @DebeziumDefault("pgoutput")
  private String pluginName;

  private String slotName;

  private Boolean slotDropOnStop;

  private String publicationName;

  private Boolean skipMessagesWithoutChange;

  private String timePrecisionMode;

  private String decimalHandlingMode;

  private String hstoreHandlingMode;

  private String intervalHandlingMode;

  private Boolean tombstonesOnDelete;

  private List<String> columnTruncateToLengthChars;

  private List<String> columnMaskWithLengthChars;

  @DebeziumMapOptions(separatorLevel1 = ".with.salt.")
  private Map<String, Map<String, List<String>>> columnMaskHash;

  @DebeziumMapOptions(separatorLevel1 = ".with.salt.")
  private Map<String, Map<String, List<String>>> columnMaskHashV2;

  @DebeziumDefault(".*")
  private List<String> columnPropagateSourceType;

  @DebeziumDefault(".*")
  private List<String> datatypePropagateSourceType;

  private List<String> messageKeyColumns;

  private String publicationAutocreateMode;

  private List<String> replicaIdentityAutosetValues;

  private String binaryHandlingMode;

  private String schemaNameAdjustmentMode;

  private String fieldNameAdjustmentMode;

  private Integer moneyFractionDigits;

  @DebeziumMapOptions(generateSummary = true, prefixFromLevel = 1)
  private Map<String, Map<String, String>> converters;

  private String snapshotMode;

  private Boolean snapshotModeConfigurationBasedSnapshotData;

  private Boolean snapshotModeConfigurationBasedSnapshotSchema;

  private Boolean snapshotModeConfigurationBasedStartStream;

  private Boolean snapshotModeConfigurationBasedSnapshotOnSchemaError;

  private Boolean snapshotModeConfigurationBasedSnapshotOnDataError;

  private String snapshotModeCustomName;

  private String snapshotLockingMode;

  private String snapshotLockingModeCustomName;

  private String snapshotQueryMode;

  private String snapshotQueryModeCustomName;

  private List<String> snapshotIncludeCollectionList;

  private Integer snapshotLockTimeoutMs;

  @DebeziumMapOptions(generateSummary = true)
  private Map<String, String> snapshotSelectStatementOverrides;

  private String eventProcessingFailureHandlingMode;

  private Integer maxBatchSize;

  private Integer maxQueueSize;

  private Integer maxQueueSizeInBytes;

  private Integer pollIntervalMs;

  @DebeziumDefault("true")
  private Boolean includeUnknownDatatypes;

  @DebeziumListSeparator(";")
  private List<String> databaseInitialStatements;

  private Integer statusUpdateIntervalMs;

  private Integer heartbeatIntervalMs;

  private String heartbeatActionQuery;

  private String schemaRefreshMode;

  private Integer snapshotDelayMs;

  private Integer snapshotFetchSize;

  @DebeziumListSeparator(";")
  @DebeziumMapOptions(separatorLevel0 = "=", valueFromLevel = 0)
  private Map<String, String> slotStreamParams;

  private Integer slotMaxRetries;

  private Integer slotRetryDelayMs;

  private String unavailableValuePlaceholder;

  private Boolean provideTransactionMetadata;

  private Boolean flushLsnSource;

  private Integer retriableRestartConnectorWaitMs;

  private List<String> skippedOperations;

  private String signalDataCollection;

  @DebeziumDefault("sgstream-annotations")
  private List<String> signalEnabledChannels;

  private List<String> notificationEnabledChannels;

  private Integer incrementalSnapshotChunkSize;

  private String incrementalSnapshotWatermarkingStrategy;

  private Integer xminFetchIntervalMs;

  private String topicNamingStrategy;

  private String topicDelimiter;

  private Integer topicCacheSize;

  private String topicHeartbeatPrefix;

  private String topicTransaction;

  private Integer snapshotMaxThreads;

  @DebeziumMapOptions(separatorLevel0 = "=", valueFromLevel = 0)
  private Map<String, String> customMetricTags;

  private Integer errorsMaxRetries;

  public String getPluginName() {
    return pluginName;
  }

  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }

  public String getSlotName() {
    return slotName;
  }

  public void setSlotName(String slotName) {
    this.slotName = slotName;
  }

  public Boolean getSlotDropOnStop() {
    return slotDropOnStop;
  }

  public void setSlotDropOnStop(Boolean slotDropOnStop) {
    this.slotDropOnStop = slotDropOnStop;
  }

  public String getPublicationName() {
    return publicationName;
  }

  public void setPublicationName(String publicationName) {
    this.publicationName = publicationName;
  }

  public Boolean getSkipMessagesWithoutChange() {
    return skipMessagesWithoutChange;
  }

  public void setSkipMessagesWithoutChange(Boolean skipMessagesWithoutChange) {
    this.skipMessagesWithoutChange = skipMessagesWithoutChange;
  }

  public String getTimePrecisionMode() {
    return timePrecisionMode;
  }

  public void setTimePrecisionMode(String timePrecisionMode) {
    this.timePrecisionMode = timePrecisionMode;
  }

  public String getDecimalHandlingMode() {
    return decimalHandlingMode;
  }

  public void setDecimalHandlingMode(String decimalHandlingMode) {
    this.decimalHandlingMode = decimalHandlingMode;
  }

  public String getHstoreHandlingMode() {
    return hstoreHandlingMode;
  }

  public void setHstoreHandlingMode(String hstoreHandlingMode) {
    this.hstoreHandlingMode = hstoreHandlingMode;
  }

  public String getIntervalHandlingMode() {
    return intervalHandlingMode;
  }

  public void setIntervalHandlingMode(String intervalHandlingMode) {
    this.intervalHandlingMode = intervalHandlingMode;
  }

  public Boolean getTombstonesOnDelete() {
    return tombstonesOnDelete;
  }

  public void setTombstonesOnDelete(Boolean tombstonesOnDelete) {
    this.tombstonesOnDelete = tombstonesOnDelete;
  }

  public List<String> getColumnTruncateToLengthChars() {
    return columnTruncateToLengthChars;
  }

  public void setColumnTruncateToLengthChars(List<String> columnTruncateToLengthChars) {
    this.columnTruncateToLengthChars = columnTruncateToLengthChars;
  }

  public List<String> getColumnMaskWithLengthChars() {
    return columnMaskWithLengthChars;
  }

  public void setColumnMaskWithLengthChars(List<String> columnMaskWithLengthChars) {
    this.columnMaskWithLengthChars = columnMaskWithLengthChars;
  }

  public Map<String, Map<String, List<String>>> getColumnMaskHash() {
    return columnMaskHash;
  }

  public void setColumnMaskHash(Map<String, Map<String, List<String>>> columnMaskHash) {
    this.columnMaskHash = columnMaskHash;
  }

  public Map<String, Map<String, List<String>>> getColumnMaskHashV2() {
    return columnMaskHashV2;
  }

  public void setColumnMaskHashV2(Map<String, Map<String, List<String>>> columnMaskHashV2) {
    this.columnMaskHashV2 = columnMaskHashV2;
  }

  public List<String> getColumnPropagateSourceType() {
    return columnPropagateSourceType;
  }

  public void setColumnPropagateSourceType(List<String> columnPropagateSourceType) {
    this.columnPropagateSourceType = columnPropagateSourceType;
  }

  public List<String> getDatatypePropagateSourceType() {
    return datatypePropagateSourceType;
  }

  public void setDatatypePropagateSourceType(List<String> datatypePropagateSourceType) {
    this.datatypePropagateSourceType = datatypePropagateSourceType;
  }

  public List<String> getMessageKeyColumns() {
    return messageKeyColumns;
  }

  public void setMessageKeyColumns(List<String> messageKeyColumns) {
    this.messageKeyColumns = messageKeyColumns;
  }

  public String getPublicationAutocreateMode() {
    return publicationAutocreateMode;
  }

  public void setPublicationAutocreateMode(String publicationAutocreateMode) {
    this.publicationAutocreateMode = publicationAutocreateMode;
  }

  public List<String> getReplicaIdentityAutosetValues() {
    return replicaIdentityAutosetValues;
  }

  public void setReplicaIdentityAutosetValues(List<String> replicaIdentityAutosetValues) {
    this.replicaIdentityAutosetValues = replicaIdentityAutosetValues;
  }

  public String getBinaryHandlingMode() {
    return binaryHandlingMode;
  }

  public void setBinaryHandlingMode(String binaryHandlingMode) {
    this.binaryHandlingMode = binaryHandlingMode;
  }

  public String getSchemaNameAdjustmentMode() {
    return schemaNameAdjustmentMode;
  }

  public void setSchemaNameAdjustmentMode(String schemaNameAdjustmentMode) {
    this.schemaNameAdjustmentMode = schemaNameAdjustmentMode;
  }

  public String getFieldNameAdjustmentMode() {
    return fieldNameAdjustmentMode;
  }

  public void setFieldNameAdjustmentMode(String fieldNameAdjustmentMode) {
    this.fieldNameAdjustmentMode = fieldNameAdjustmentMode;
  }

  public Integer getMoneyFractionDigits() {
    return moneyFractionDigits;
  }

  public void setMoneyFractionDigits(Integer moneyFractionDigits) {
    this.moneyFractionDigits = moneyFractionDigits;
  }

  public Map<String, Map<String, String>> getConverters() {
    return converters;
  }

  public void setConverters(Map<String, Map<String, String>> converters) {
    this.converters = converters;
  }

  public String getSnapshotMode() {
    return snapshotMode;
  }

  public void setSnapshotMode(String snapshotMode) {
    this.snapshotMode = snapshotMode;
  }

  public Boolean getSnapshotModeConfigurationBasedSnapshotData() {
    return snapshotModeConfigurationBasedSnapshotData;
  }

  public void setSnapshotModeConfigurationBasedSnapshotData(
      Boolean snapshotModeConfigurationBasedSnapshotData) {
    this.snapshotModeConfigurationBasedSnapshotData = snapshotModeConfigurationBasedSnapshotData;
  }

  public Boolean getSnapshotModeConfigurationBasedSnapshotSchema() {
    return snapshotModeConfigurationBasedSnapshotSchema;
  }

  public void setSnapshotModeConfigurationBasedSnapshotSchema(
      Boolean snapshotModeConfigurationBasedSnapshotSchema) {
    this.snapshotModeConfigurationBasedSnapshotSchema = snapshotModeConfigurationBasedSnapshotSchema;
  }

  public Boolean getSnapshotModeConfigurationBasedStartStream() {
    return snapshotModeConfigurationBasedStartStream;
  }

  public void setSnapshotModeConfigurationBasedStartStream(
      Boolean snapshotModeConfigurationBasedStartStream) {
    this.snapshotModeConfigurationBasedStartStream = snapshotModeConfigurationBasedStartStream;
  }

  public Boolean getSnapshotModeConfigurationBasedSnapshotOnSchemaError() {
    return snapshotModeConfigurationBasedSnapshotOnSchemaError;
  }

  public void setSnapshotModeConfigurationBasedSnapshotOnSchemaError(
      Boolean snapshotModeConfigurationBasedSnapshotOnSchemaError) {
    this.snapshotModeConfigurationBasedSnapshotOnSchemaError = snapshotModeConfigurationBasedSnapshotOnSchemaError;
  }

  public Boolean getSnapshotModeConfigurationBasedSnapshotOnDataError() {
    return snapshotModeConfigurationBasedSnapshotOnDataError;
  }

  public void setSnapshotModeConfigurationBasedSnapshotOnDataError(
      Boolean snapshotModeConfigurationBasedSnapshotOnDataError) {
    this.snapshotModeConfigurationBasedSnapshotOnDataError = snapshotModeConfigurationBasedSnapshotOnDataError;
  }

  public String getSnapshotModeCustomName() {
    return snapshotModeCustomName;
  }

  public void setSnapshotModeCustomName(String snapshotModeCustomName) {
    this.snapshotModeCustomName = snapshotModeCustomName;
  }

  public String getSnapshotLockingMode() {
    return snapshotLockingMode;
  }

  public void setSnapshotLockingMode(String snapshotLockingMode) {
    this.snapshotLockingMode = snapshotLockingMode;
  }

  public String getSnapshotLockingModeCustomName() {
    return snapshotLockingModeCustomName;
  }

  public void setSnapshotLockingModeCustomName(String snapshotLockingModeCustomName) {
    this.snapshotLockingModeCustomName = snapshotLockingModeCustomName;
  }

  public String getSnapshotQueryMode() {
    return snapshotQueryMode;
  }

  public void setSnapshotQueryMode(String snapshotQueryMode) {
    this.snapshotQueryMode = snapshotQueryMode;
  }

  public String getSnapshotQueryModeCustomName() {
    return snapshotQueryModeCustomName;
  }

  public void setSnapshotQueryModeCustomName(String snapshotQueryModeCustomName) {
    this.snapshotQueryModeCustomName = snapshotQueryModeCustomName;
  }

  public List<String> getSnapshotIncludeCollectionList() {
    return snapshotIncludeCollectionList;
  }

  public void setSnapshotIncludeCollectionList(List<String> snapshotIncludeCollectionList) {
    this.snapshotIncludeCollectionList = snapshotIncludeCollectionList;
  }

  public Integer getSnapshotLockTimeoutMs() {
    return snapshotLockTimeoutMs;
  }

  public void setSnapshotLockTimeoutMs(Integer snapshotLockTimeoutMs) {
    this.snapshotLockTimeoutMs = snapshotLockTimeoutMs;
  }

  public Map<String, String> getSnapshotSelectStatementOverrides() {
    return snapshotSelectStatementOverrides;
  }

  public void setSnapshotSelectStatementOverrides(
      Map<String, String> snapshotSelectStatementOverrides) {
    this.snapshotSelectStatementOverrides = snapshotSelectStatementOverrides;
  }

  public String getEventProcessingFailureHandlingMode() {
    return eventProcessingFailureHandlingMode;
  }

  public void setEventProcessingFailureHandlingMode(String eventProcessingFailureHandlingMode) {
    this.eventProcessingFailureHandlingMode = eventProcessingFailureHandlingMode;
  }

  public Integer getMaxBatchSize() {
    return maxBatchSize;
  }

  public void setMaxBatchSize(Integer maxBatchSize) {
    this.maxBatchSize = maxBatchSize;
  }

  public Integer getMaxQueueSize() {
    return maxQueueSize;
  }

  public void setMaxQueueSize(Integer maxQueueSize) {
    this.maxQueueSize = maxQueueSize;
  }

  public Integer getMaxQueueSizeInBytes() {
    return maxQueueSizeInBytes;
  }

  public void setMaxQueueSizeInBytes(Integer maxQueueSizeInBytes) {
    this.maxQueueSizeInBytes = maxQueueSizeInBytes;
  }

  public Integer getPollIntervalMs() {
    return pollIntervalMs;
  }

  public void setPollIntervalMs(Integer pollIntervalMs) {
    this.pollIntervalMs = pollIntervalMs;
  }

  public Boolean getIncludeUnknownDatatypes() {
    return includeUnknownDatatypes;
  }

  public void setIncludeUnknownDatatypes(Boolean includeUnknownDatatypes) {
    this.includeUnknownDatatypes = includeUnknownDatatypes;
  }

  public List<String> getDatabaseInitialStatements() {
    return databaseInitialStatements;
  }

  public void setDatabaseInitialStatements(List<String> databaseInitialStatements) {
    this.databaseInitialStatements = databaseInitialStatements;
  }

  public Integer getStatusUpdateIntervalMs() {
    return statusUpdateIntervalMs;
  }

  public void setStatusUpdateIntervalMs(Integer statusUpdateIntervalMs) {
    this.statusUpdateIntervalMs = statusUpdateIntervalMs;
  }

  public Integer getHeartbeatIntervalMs() {
    return heartbeatIntervalMs;
  }

  public void setHeartbeatIntervalMs(Integer heartbeatIntervalMs) {
    this.heartbeatIntervalMs = heartbeatIntervalMs;
  }

  public String getHeartbeatActionQuery() {
    return heartbeatActionQuery;
  }

  public void setHeartbeatActionQuery(String heartbeatActionQuery) {
    this.heartbeatActionQuery = heartbeatActionQuery;
  }

  public String getSchemaRefreshMode() {
    return schemaRefreshMode;
  }

  public void setSchemaRefreshMode(String schemaRefreshMode) {
    this.schemaRefreshMode = schemaRefreshMode;
  }

  public Integer getSnapshotDelayMs() {
    return snapshotDelayMs;
  }

  public void setSnapshotDelayMs(Integer snapshotDelayMs) {
    this.snapshotDelayMs = snapshotDelayMs;
  }

  public Integer getSnapshotFetchSize() {
    return snapshotFetchSize;
  }

  public void setSnapshotFetchSize(Integer snapshotFetchSize) {
    this.snapshotFetchSize = snapshotFetchSize;
  }

  public Map<String, String> getSlotStreamParams() {
    return slotStreamParams;
  }

  public void setSlotStreamParams(Map<String, String> slotStreamParams) {
    this.slotStreamParams = slotStreamParams;
  }

  public Integer getSlotMaxRetries() {
    return slotMaxRetries;
  }

  public void setSlotMaxRetries(Integer slotMaxRetries) {
    this.slotMaxRetries = slotMaxRetries;
  }

  public Integer getSlotRetryDelayMs() {
    return slotRetryDelayMs;
  }

  public void setSlotRetryDelayMs(Integer slotRetryDelayMs) {
    this.slotRetryDelayMs = slotRetryDelayMs;
  }

  public String getUnavailableValuePlaceholder() {
    return unavailableValuePlaceholder;
  }

  public void setUnavailableValuePlaceholder(String unavailableValuePlaceholder) {
    this.unavailableValuePlaceholder = unavailableValuePlaceholder;
  }

  public Boolean getProvideTransactionMetadata() {
    return provideTransactionMetadata;
  }

  public void setProvideTransactionMetadata(Boolean provideTransactionMetadata) {
    this.provideTransactionMetadata = provideTransactionMetadata;
  }

  public Boolean getFlushLsnSource() {
    return flushLsnSource;
  }

  public void setFlushLsnSource(Boolean flushLsnSource) {
    this.flushLsnSource = flushLsnSource;
  }

  public Integer getRetriableRestartConnectorWaitMs() {
    return retriableRestartConnectorWaitMs;
  }

  public void setRetriableRestartConnectorWaitMs(Integer retriableRestartConnectorWaitMs) {
    this.retriableRestartConnectorWaitMs = retriableRestartConnectorWaitMs;
  }

  public List<String> getSkippedOperations() {
    return skippedOperations;
  }

  public void setSkippedOperations(List<String> skippedOperations) {
    this.skippedOperations = skippedOperations;
  }

  public String getSignalDataCollection() {
    return signalDataCollection;
  }

  public void setSignalDataCollection(String signalDataCollection) {
    this.signalDataCollection = signalDataCollection;
  }

  public List<String> getSignalEnabledChannels() {
    return signalEnabledChannels;
  }

  public void setSignalEnabledChannels(List<String> signalEnabledChannels) {
    this.signalEnabledChannels = signalEnabledChannels;
  }

  public List<String> getNotificationEnabledChannels() {
    return notificationEnabledChannels;
  }

  public void setNotificationEnabledChannels(List<String> notificationEnabledChannels) {
    this.notificationEnabledChannels = notificationEnabledChannels;
  }

  public Integer getIncrementalSnapshotChunkSize() {
    return incrementalSnapshotChunkSize;
  }

  public void setIncrementalSnapshotChunkSize(Integer incrementalSnapshotChunkSize) {
    this.incrementalSnapshotChunkSize = incrementalSnapshotChunkSize;
  }

  public String getIncrementalSnapshotWatermarkingStrategy() {
    return incrementalSnapshotWatermarkingStrategy;
  }

  public void setIncrementalSnapshotWatermarkingStrategy(
      String incrementalSnapshotWatermarkingStrategy) {
    this.incrementalSnapshotWatermarkingStrategy = incrementalSnapshotWatermarkingStrategy;
  }

  public Integer getXminFetchIntervalMs() {
    return xminFetchIntervalMs;
  }

  public void setXminFetchIntervalMs(Integer xminFetchIntervalMs) {
    this.xminFetchIntervalMs = xminFetchIntervalMs;
  }

  public String getTopicNamingStrategy() {
    return topicNamingStrategy;
  }

  public void setTopicNamingStrategy(String topicNamingStrategy) {
    this.topicNamingStrategy = topicNamingStrategy;
  }

  public String getTopicDelimiter() {
    return topicDelimiter;
  }

  public void setTopicDelimiter(String topicDelimiter) {
    this.topicDelimiter = topicDelimiter;
  }

  public Integer getTopicCacheSize() {
    return topicCacheSize;
  }

  public void setTopicCacheSize(Integer topicCacheSize) {
    this.topicCacheSize = topicCacheSize;
  }

  public String getTopicHeartbeatPrefix() {
    return topicHeartbeatPrefix;
  }

  public void setTopicHeartbeatPrefix(String topicHeartbeatPrefix) {
    this.topicHeartbeatPrefix = topicHeartbeatPrefix;
  }

  public String getTopicTransaction() {
    return topicTransaction;
  }

  public void setTopicTransaction(String topicTransaction) {
    this.topicTransaction = topicTransaction;
  }

  public Integer getSnapshotMaxThreads() {
    return snapshotMaxThreads;
  }

  public void setSnapshotMaxThreads(Integer snapshotMaxThreads) {
    this.snapshotMaxThreads = snapshotMaxThreads;
  }

  public Map<String, String> getCustomMetricTags() {
    return customMetricTags;
  }

  public void setCustomMetricTags(Map<String, String> customMetricTags) {
    this.customMetricTags = customMetricTags;
  }

  public Integer getErrorsMaxRetries() {
    return errorsMaxRetries;
  }

  public void setErrorsMaxRetries(Integer errorsMaxRetries) {
    this.errorsMaxRetries = errorsMaxRetries;
  }

  @Override
  public int hashCode() {
    return Objects.hash(binaryHandlingMode, columnMaskHash, columnMaskHashV2,
        columnMaskWithLengthChars, columnPropagateSourceType, columnTruncateToLengthChars,
        converters, customMetricTags, databaseInitialStatements, datatypePropagateSourceType,
        decimalHandlingMode, errorsMaxRetries, eventProcessingFailureHandlingMode,
        fieldNameAdjustmentMode, flushLsnSource, heartbeatActionQuery, heartbeatIntervalMs,
        hstoreHandlingMode, includeUnknownDatatypes, incrementalSnapshotChunkSize,
        incrementalSnapshotWatermarkingStrategy, intervalHandlingMode, maxBatchSize, maxQueueSize,
        maxQueueSizeInBytes, messageKeyColumns, moneyFractionDigits, notificationEnabledChannels,
        pluginName, pollIntervalMs, provideTransactionMetadata, publicationAutocreateMode,
        publicationName, replicaIdentityAutosetValues, retriableRestartConnectorWaitMs,
        schemaNameAdjustmentMode, schemaRefreshMode, signalDataCollection, signalEnabledChannels,
        skipMessagesWithoutChange, skippedOperations, slotDropOnStop, slotMaxRetries, slotName,
        slotRetryDelayMs, slotStreamParams, snapshotDelayMs, snapshotFetchSize,
        snapshotIncludeCollectionList, snapshotLockTimeoutMs, snapshotLockingMode,
        snapshotLockingModeCustomName, snapshotMaxThreads, snapshotMode,
        snapshotModeConfigurationBasedSnapshotData,
        snapshotModeConfigurationBasedSnapshotOnDataError,
        snapshotModeConfigurationBasedSnapshotOnSchemaError,
        snapshotModeConfigurationBasedSnapshotSchema, snapshotModeConfigurationBasedStartStream,
        snapshotModeCustomName, snapshotQueryMode, snapshotQueryModeCustomName,
        snapshotSelectStatementOverrides, statusUpdateIntervalMs, timePrecisionMode,
        tombstonesOnDelete, topicCacheSize, topicDelimiter, topicHeartbeatPrefix,
        topicNamingStrategy, topicTransaction, unavailableValuePlaceholder, xminFetchIntervalMs);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamSourcePostgresDebeziumProperties)) {
      return false;
    }
    StackGresStreamSourcePostgresDebeziumProperties other = (StackGresStreamSourcePostgresDebeziumProperties) obj;
    return Objects.equals(binaryHandlingMode, other.binaryHandlingMode)
        && Objects.equals(columnMaskHash, other.columnMaskHash)
        && Objects.equals(columnMaskHashV2, other.columnMaskHashV2)
        && Objects.equals(columnMaskWithLengthChars, other.columnMaskWithLengthChars)
        && Objects.equals(columnPropagateSourceType, other.columnPropagateSourceType)
        && Objects.equals(columnTruncateToLengthChars, other.columnTruncateToLengthChars)
        && Objects.equals(converters, other.converters)
        && Objects.equals(customMetricTags, other.customMetricTags)
        && Objects.equals(databaseInitialStatements, other.databaseInitialStatements)
        && Objects.equals(datatypePropagateSourceType, other.datatypePropagateSourceType)
        && Objects.equals(decimalHandlingMode, other.decimalHandlingMode)
        && Objects.equals(errorsMaxRetries, other.errorsMaxRetries)
        && Objects.equals(eventProcessingFailureHandlingMode,
            other.eventProcessingFailureHandlingMode)
        && Objects.equals(fieldNameAdjustmentMode, other.fieldNameAdjustmentMode)
        && Objects.equals(flushLsnSource, other.flushLsnSource)
        && Objects.equals(heartbeatActionQuery, other.heartbeatActionQuery)
        && Objects.equals(heartbeatIntervalMs, other.heartbeatIntervalMs)
        && Objects.equals(hstoreHandlingMode, other.hstoreHandlingMode)
        && Objects.equals(includeUnknownDatatypes, other.includeUnknownDatatypes)
        && Objects.equals(incrementalSnapshotChunkSize, other.incrementalSnapshotChunkSize)
        && Objects.equals(incrementalSnapshotWatermarkingStrategy,
            other.incrementalSnapshotWatermarkingStrategy)
        && Objects.equals(intervalHandlingMode, other.intervalHandlingMode)
        && Objects.equals(maxBatchSize, other.maxBatchSize)
        && Objects.equals(maxQueueSize, other.maxQueueSize)
        && Objects.equals(maxQueueSizeInBytes, other.maxQueueSizeInBytes)
        && Objects.equals(messageKeyColumns, other.messageKeyColumns)
        && Objects.equals(moneyFractionDigits, other.moneyFractionDigits)
        && Objects.equals(notificationEnabledChannels, other.notificationEnabledChannels)
        && Objects.equals(pluginName, other.pluginName)
        && Objects.equals(pollIntervalMs, other.pollIntervalMs)
        && Objects.equals(provideTransactionMetadata, other.provideTransactionMetadata)
        && Objects.equals(publicationAutocreateMode, other.publicationAutocreateMode)
        && Objects.equals(publicationName, other.publicationName)
        && Objects.equals(replicaIdentityAutosetValues, other.replicaIdentityAutosetValues)
        && Objects.equals(retriableRestartConnectorWaitMs, other.retriableRestartConnectorWaitMs)
        && Objects.equals(schemaNameAdjustmentMode, other.schemaNameAdjustmentMode)
        && Objects.equals(schemaRefreshMode, other.schemaRefreshMode)
        && Objects.equals(signalDataCollection, other.signalDataCollection)
        && Objects.equals(signalEnabledChannels, other.signalEnabledChannels)
        && Objects.equals(skipMessagesWithoutChange, other.skipMessagesWithoutChange)
        && Objects.equals(skippedOperations, other.skippedOperations)
        && Objects.equals(slotDropOnStop, other.slotDropOnStop)
        && Objects.equals(slotMaxRetries, other.slotMaxRetries)
        && Objects.equals(slotName, other.slotName)
        && Objects.equals(slotRetryDelayMs, other.slotRetryDelayMs)
        && Objects.equals(slotStreamParams, other.slotStreamParams)
        && Objects.equals(snapshotDelayMs, other.snapshotDelayMs)
        && Objects.equals(snapshotFetchSize, other.snapshotFetchSize)
        && Objects.equals(snapshotIncludeCollectionList, other.snapshotIncludeCollectionList)
        && Objects.equals(snapshotLockTimeoutMs, other.snapshotLockTimeoutMs)
        && Objects.equals(snapshotLockingMode, other.snapshotLockingMode)
        && Objects.equals(snapshotLockingModeCustomName, other.snapshotLockingModeCustomName)
        && Objects.equals(snapshotMaxThreads, other.snapshotMaxThreads)
        && Objects.equals(snapshotMode, other.snapshotMode)
        && Objects.equals(snapshotModeConfigurationBasedSnapshotData,
            other.snapshotModeConfigurationBasedSnapshotData)
        && Objects.equals(snapshotModeConfigurationBasedSnapshotOnDataError,
            other.snapshotModeConfigurationBasedSnapshotOnDataError)
        && Objects.equals(snapshotModeConfigurationBasedSnapshotOnSchemaError,
            other.snapshotModeConfigurationBasedSnapshotOnSchemaError)
        && Objects.equals(snapshotModeConfigurationBasedSnapshotSchema,
            other.snapshotModeConfigurationBasedSnapshotSchema)
        && Objects.equals(snapshotModeConfigurationBasedStartStream,
            other.snapshotModeConfigurationBasedStartStream)
        && Objects.equals(snapshotModeCustomName, other.snapshotModeCustomName)
        && Objects.equals(snapshotQueryMode, other.snapshotQueryMode)
        && Objects.equals(snapshotQueryModeCustomName, other.snapshotQueryModeCustomName)
        && Objects.equals(snapshotSelectStatementOverrides, other.snapshotSelectStatementOverrides)
        && Objects.equals(statusUpdateIntervalMs, other.statusUpdateIntervalMs)
        && Objects.equals(timePrecisionMode, other.timePrecisionMode)
        && Objects.equals(tombstonesOnDelete, other.tombstonesOnDelete)
        && Objects.equals(topicCacheSize, other.topicCacheSize)
        && Objects.equals(topicDelimiter, other.topicDelimiter)
        && Objects.equals(topicHeartbeatPrefix, other.topicHeartbeatPrefix)
        && Objects.equals(topicNamingStrategy, other.topicNamingStrategy)
        && Objects.equals(topicTransaction, other.topicTransaction)
        && Objects.equals(unavailableValuePlaceholder, other.unavailableValuePlaceholder)
        && Objects.equals(xminFetchIntervalMs, other.xminFetchIntervalMs);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
