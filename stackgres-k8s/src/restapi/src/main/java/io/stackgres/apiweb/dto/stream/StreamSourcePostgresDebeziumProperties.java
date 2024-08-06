/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamSourcePostgresDebeziumProperties {

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

  private Map<String, Map<String, List<String>>> columnMaskHash;

  private Map<String, Map<String, List<String>>> columnMaskHashV2;

  private List<String> columnPropagateSourceType;

  private List<String> datatypePropagateSourceType;

  private List<String> messageKeyColumns;

  private String publicationAutocreateMode;

  private List<String> replicaIdentityAutosetValues;

  private String binaryHandlingMode;

  private String schemaNameAdjustmentMode;

  private String fieldNameAdjustmentMode;

  private Integer moneyFractionDigits;

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

  private Map<String, String> snapshotSelectStatementOverrides;

  private String eventProcessingFailureHandlingMode;

  private Integer maxBatchSize;

  private Integer maxQueueSize;

  private Integer maxQueueSizeInBytes;

  private Integer pollIntervalMs;

  private Boolean includeUnknownDatatypes;

  private List<String> databaseInitialStatements;

  private Integer statusUpdateIntervalMs;

  private Integer heartbeatIntervalMs;

  private String heartbeatActionQuery;

  private String schemaRefreshMode;

  private Integer snapshotDelayMs;

  private Integer snapshotFetchSize;

  private Map<String, String> slotStreamParams;

  private Integer slotMaxRetries;

  private Integer slotRetryDelayMs;

  private String unavailableValuePlaceholder;

  private Boolean provideTransactionMetadata;

  private Boolean flushLsnSource;

  private Integer retriableRestartConnectorWaitMs;

  private List<String> skippedOperations;

  private String signalDataCollection;

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
