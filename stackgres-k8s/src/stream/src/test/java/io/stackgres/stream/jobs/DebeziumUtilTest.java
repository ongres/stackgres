/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamDebeziumEngineProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgresDebeziumProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetJdbcSinkDebeziumProperties;
import io.stackgres.testutil.ModelTestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DebeziumUtilTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumUtilTest.class);

  @Test
  void givenAExpectedStream_shouldExtractDebeziumEnginePropertiesCorrectly() {
    StackGresStream stream = ModelTestUtil.createWithRandomData(StackGresStream.class, 2);
    Properties props = new Properties();
    StackGresStreamDebeziumEngineProperties streamProperties = stream.getSpec().getDebeziumEngineProperties();
    DebeziumUtil.configureDebeziumSectionProperties(
        props,
        streamProperties,
        StackGresStreamDebeziumEngineProperties.class);
    LOGGER.info("Properties for StackGresStreamDebeziumEngineProperties:\n{}",
        props.entrySet().stream()
        .sorted(Comparator.comparing(e -> e.getKey().toString()))
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("\n")));
    Map<String, Object> expectedProperties = new HashMap<>(Map.ofEntries(
        assertEntryInProperties(props, Map.entry("errors.max.retries", streamProperties.getErrorsMaxRetries())),
        assertEntryInProperties(
            props, Map.entry("errors.retry.delay.initial.ms", streamProperties.getErrorsRetryDelayInitialMs())),
        assertEntryInProperties(
            props, Map.entry("errors.retry.delay.max.ms", streamProperties.getErrorsRetryDelayMaxMs())),
        assertEntryInProperties(props, Map.entry("offset.commit.policy", streamProperties.getOffsetCommitPolicy())),
        assertEntryInProperties(
            props, Map.entry("offset.flush.interval.ms", streamProperties.getOffsetFlushIntervalMs())),
        assertEntryInProperties(
            props, Map.entry("offset.flush.timeout.ms", streamProperties.getOffsetFlushTimeoutMs())),
        assertEntryInProperties(props, Map.entry(
            "predicates",
            streamProperties.getPredicates().keySet().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "predicates." + streamProperties.getPredicates()
            .keySet().stream().toList().get(0)
            + "." + streamProperties.getPredicates()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(0),
            streamProperties.getPredicates()
            .values().stream().toList().get(0)
            .values().stream().toList().get(0))),
        assertEntryInProperties(props, Map.entry(
            "predicates." + streamProperties.getPredicates()
            .keySet().stream().toList().get(0)
            + "." + streamProperties.getPredicates()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(1),
            streamProperties.getPredicates()
            .values().stream().toList().get(0)
            .values().stream().toList().get(1))),
        assertEntryInProperties(props, Map.entry(
            "predicates." + streamProperties.getPredicates()
            .keySet().stream().toList().get(1)
            + "." + streamProperties.getPredicates()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(0),
            streamProperties.getPredicates()
            .values().stream().toList().get(1)
            .values().stream().toList().get(0))),
        assertEntryInProperties(props, Map.entry(
            "predicates." + streamProperties.getPredicates()
            .keySet().stream().toList().get(1)
            + "." + streamProperties.getPredicates()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(1),
            streamProperties.getPredicates()
            .values().stream().toList().get(1)
            .values().stream().toList().get(1))),
        assertEntryInProperties(props, Map.entry(
            "transforms",
            streamProperties.getTransforms().keySet().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "transforms." + streamProperties.getTransforms()
            .keySet().stream().toList().get(0)
            + "." + streamProperties.getTransforms()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(0),
            streamProperties.getTransforms()
            .values().stream().toList().get(0)
            .values().stream().toList().get(0))),
        assertEntryInProperties(props, Map.entry(
            "transforms." + streamProperties.getTransforms()
            .keySet().stream().toList().get(0)
            + "." + streamProperties.getTransforms()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(1),
            streamProperties.getTransforms()
            .values().stream().toList().get(0)
            .values().stream().toList().get(1))),
        assertEntryInProperties(props, Map.entry(
            "transforms." + streamProperties.getTransforms()
            .keySet().stream().toList().get(1)
            + "." + streamProperties.getTransforms()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(0),
            streamProperties.getTransforms()
            .values().stream().toList().get(1)
            .values().stream().toList().get(0))),
        assertEntryInProperties(props, Map.entry(
            "transforms." + streamProperties.getTransforms()
            .keySet().stream().toList().get(1)
            + "." + streamProperties.getTransforms()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(1),
            streamProperties.getTransforms()
            .values().stream().toList().get(1)
            .values().stream().toList().get(1))),
        assertEntryInProperties(
            props, Map.entry(
                "record.processing.threads",
                streamProperties.getRecordProcessingThreads())),
        assertEntryInProperties(
            props, Map.entry(
                "record.processing.with.serial.consumer",
                streamProperties.getRecordProcessingWithSerialConsumer())),
        assertEntryInProperties(
            props, Map.entry(
                "record.processing.order",
                streamProperties.getRecordProcessingOrder())),
        assertEntryInProperties(
            props, Map.entry(
                "record.processing.shutdown.timeout.ms",
                streamProperties.getRecordProcessingShutdownTimeoutMs())),
        assertEntryInProperties(
            props, Map.entry(
                "task.management.timeout.ms",
                streamProperties.getTaskManagementTimeoutMs())),
        // Leave this so we can order all the properties correctly without bothering for the latest `,`
        Map.entry("|", streamProperties)
        ));
    props.forEach((key, value) -> {
      Assertions.assertTrue(expectedProperties.containsKey(key), "expected not found for property " + key);
    });
  }

  @Test
  void givenAExpectedStream_shouldExtractDebeziumPostgresConnectorPropertiesCorrectly() {
    StackGresStream stream = ModelTestUtil.createWithRandomData(StackGresStream.class, 2);
    Properties props = new Properties();
    StackGresStreamSourcePostgresDebeziumProperties streamProperties =
        stream.getSpec().getSource().getSgCluster().getDebeziumProperties();
    DebeziumUtil.configureDebeziumSectionProperties(
        props,
        streamProperties,
        StackGresStreamSourcePostgresDebeziumProperties.class);
    LOGGER.info("Properties for StackGresStreamSourcePostgresDebeziumProperties:\n{}",
        props.entrySet().stream()
        .sorted(Comparator.comparing(e -> e.getKey().toString()))
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("\n")));
    Map<String, Object> expectedProperties = new HashMap<>(Map.ofEntries(
        assertEntryInProperties(props, Map.entry("binary.handling.mode", streamProperties.getBinaryHandlingMode())),
        assertEntryInProperties(props, Map.entry(
            "column.mask.hash." + streamProperties.getColumnMaskHash()
            .keySet().stream().toList().get(0)
            + ".with.salt." + streamProperties.getColumnMaskHash()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(0),
            streamProperties.getColumnMaskHash()
            .values().stream().toList().get(0)
            .values().stream().toList().get(0).stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.mask.hash." + streamProperties.getColumnMaskHash()
            .keySet().stream().toList().get(0)
            + ".with.salt." + streamProperties.getColumnMaskHash()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(1),
            streamProperties.getColumnMaskHash()
            .values().stream().toList().get(0)
            .values().stream().toList().get(1).stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.mask.hash." + streamProperties.getColumnMaskHash()
            .keySet().stream().toList().get(1)
            + ".with.salt." + streamProperties.getColumnMaskHash()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(0),
            streamProperties.getColumnMaskHash()
            .values().stream().toList().get(1)
            .values().stream().toList().get(0).stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.mask.hash." + streamProperties.getColumnMaskHash()
            .keySet().stream().toList().get(1)
            + ".with.salt." + streamProperties.getColumnMaskHash()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(1),
            streamProperties.getColumnMaskHash()
            .values().stream().toList().get(1)
            .values().stream().toList().get(1).stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.mask.hash.v2." + streamProperties.getColumnMaskHashV2()
            .keySet().stream().toList().get(0)
            + ".with.salt." + streamProperties.getColumnMaskHashV2()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(0),
            streamProperties.getColumnMaskHashV2()
            .values().stream().toList().get(0)
            .values().stream().toList().get(0).stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.mask.hash.v2." + streamProperties.getColumnMaskHashV2()
            .keySet().stream().toList().get(0)
            + ".with.salt." + streamProperties.getColumnMaskHashV2()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(1),
            streamProperties.getColumnMaskHashV2()
            .values().stream().toList().get(0)
            .values().stream().toList().get(1).stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.mask.hash.v2." + streamProperties.getColumnMaskHashV2()
            .keySet().stream().toList().get(1)
            + ".with.salt." + streamProperties.getColumnMaskHashV2()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(0),
            streamProperties.getColumnMaskHashV2()
            .values().stream().toList().get(1)
            .values().stream().toList().get(0).stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.mask.hash.v2." + streamProperties.getColumnMaskHashV2()
            .keySet().stream().toList().get(1)
            + ".with.salt." + streamProperties.getColumnMaskHashV2()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(1),
            streamProperties.getColumnMaskHashV2()
            .values().stream().toList().get(1)
            .values().stream().toList().get(1).stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.mask.with.length.chars",
            streamProperties.getColumnMaskWithLengthChars().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.propagate.source.type",
            streamProperties.getColumnPropagateSourceType().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "column.truncate.to.length.chars",
            streamProperties.getColumnTruncateToLengthChars().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "converters",
            streamProperties.getConverters().keySet().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            streamProperties.getConverters()
            .keySet().stream().toList().get(0)
            + "." + streamProperties.getConverters()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(0),
            streamProperties.getConverters()
            .values().stream().toList().get(0)
            .values().stream().toList().get(0))),
        assertEntryInProperties(props, Map.entry(
            streamProperties.getConverters()
            .keySet().stream().toList().get(0)
            + "." + streamProperties.getConverters()
            .values().stream().toList().get(0)
            .keySet().stream().toList().get(1),
            streamProperties.getConverters()
            .values().stream().toList().get(0)
            .values().stream().toList().get(1))),
        assertEntryInProperties(props, Map.entry(
            streamProperties.getConverters()
            .keySet().stream().toList().get(1)
            + "." + streamProperties.getConverters()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(0),
            streamProperties.getConverters()
            .values().stream().toList().get(1)
            .values().stream().toList().get(0))),
        assertEntryInProperties(props, Map.entry(
            streamProperties.getConverters()
            .keySet().stream().toList().get(1)
            + "." + streamProperties.getConverters()
            .values().stream().toList().get(1)
            .keySet().stream().toList().get(1),
            streamProperties.getConverters()
            .values().stream().toList().get(1)
            .values().stream().toList().get(1))),
        assertEntryInProperties(props, Map.entry(
            "custom.metric.tags",
            streamProperties.getCustomMetricTags().entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "database.initial.statements",
            streamProperties.getDatabaseInitialStatements().stream().collect(Collectors.joining(";")))),
        assertEntryInProperties(props, Map.entry(
            "datatype.propagate.source.type",
            streamProperties.getDatatypePropagateSourceType().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry("decimal.handling.mode", streamProperties.getDecimalHandlingMode())),
        assertEntryInProperties(props, Map.entry("errors.max.retries", streamProperties.getErrorsMaxRetries())),
        assertEntryInProperties(props, Map.entry(
            "event.processing.failure.handling.mode", streamProperties.getEventProcessingFailureHandlingMode())),
        assertEntryInProperties(
            props, Map.entry("field.name.adjustment.mode", streamProperties.getFieldNameAdjustmentMode())),
        assertEntryInProperties(props, Map.entry("flush.lsn.source", streamProperties.getFlushLsnSource())),
        assertEntryInProperties(props, Map.entry("heartbeat.action.query", streamProperties.getHeartbeatActionQuery())),
        assertEntryInProperties(props, Map.entry("heartbeat.interval.ms", streamProperties.getHeartbeatIntervalMs())),
        assertEntryInProperties(props, Map.entry("hstore.handling.mode", streamProperties.getHstoreHandlingMode())),
        assertEntryInProperties(
            props, Map.entry("include.unknown.datatypes", streamProperties.getIncludeUnknownDatatypes())),
        assertEntryInProperties(
            props, Map.entry("incremental.snapshot.chunk.size", streamProperties.getIncrementalSnapshotChunkSize())),
        assertEntryInProperties(props, Map.entry(
            "incremental.snapshot.watermarking.strategy",
            streamProperties.getIncrementalSnapshotWatermarkingStrategy())),
        assertEntryInProperties(props, Map.entry("interval.handling.mode", streamProperties.getIntervalHandlingMode())),
        assertEntryInProperties(props, Map.entry("max.batch.size", streamProperties.getMaxBatchSize())),
        assertEntryInProperties(props, Map.entry("max.queue.size", streamProperties.getMaxQueueSize())),
        assertEntryInProperties(props, Map.entry("max.queue.size.in.bytes", streamProperties.getMaxQueueSizeInBytes())),
        assertEntryInProperties(props, Map.entry(
            "message.key.columns",
            streamProperties.getMessageKeyColumns().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry("money.fraction.digits", streamProperties.getMoneyFractionDigits())),
        assertEntryInProperties(props, Map.entry(
            "notification.enabled.channels",
            streamProperties.getNotificationEnabledChannels().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry("plugin.name", streamProperties.getPluginName())),
        assertEntryInProperties(props, Map.entry("poll.interval.ms", streamProperties.getPollIntervalMs())),
        assertEntryInProperties(
            props, Map.entry("provide.transaction.metadata", streamProperties.getProvideTransactionMetadata())),
        assertEntryInProperties(
            props, Map.entry("publication.autocreate.mode", streamProperties.getPublicationAutocreateMode())),
        assertEntryInProperties(
            props, Map.entry("publication.name", streamProperties.getPublicationName())),
        assertEntryInProperties(props, Map.entry(
            "replica.identity.autoset.values",
            streamProperties.getReplicaIdentityAutosetValues().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "retriable.restart.connector.wait.ms", streamProperties.getRetriableRestartConnectorWaitMs())),
        assertEntryInProperties(
            props, Map.entry("schema.name.adjustment.mode", streamProperties.getSchemaNameAdjustmentMode())),
        assertEntryInProperties(props, Map.entry("schema.refresh.mode", streamProperties.getSchemaRefreshMode())),
        assertEntryInProperties(props, Map.entry("signal.data.collection", streamProperties.getSignalDataCollection())),
        assertEntryInProperties(props, Map.entry(
            "signal.enabled.channels",
            streamProperties.getSignalEnabledChannels().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(
            props, Map.entry("skip.messages.without.change", streamProperties.getSkipMessagesWithoutChange())),
        assertEntryInProperties(props, Map.entry(
            "skipped.operations",
            streamProperties.getSkippedOperations().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry("slot.drop.on.stop", streamProperties.getSlotDropOnStop())),
        assertEntryInProperties(props, Map.entry("slot.max.retries", streamProperties.getSlotMaxRetries())),
        assertEntryInProperties(props, Map.entry("slot.name", streamProperties.getSlotName())),
        assertEntryInProperties(props, Map.entry("slot.retry.delay.ms", streamProperties.getSlotRetryDelayMs())),
        assertEntryInProperties(props, Map.entry(
            "slot.stream.params",
            streamProperties.getSlotStreamParams().entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(";")))),
        assertEntryInProperties(props, Map.entry("snapshot.delay.ms", streamProperties.getSnapshotDelayMs())),
        assertEntryInProperties(props, Map.entry("snapshot.fetch.size", streamProperties.getSnapshotFetchSize())),
        assertEntryInProperties(props, Map.entry(
            "snapshot.include.collection.list",
            streamProperties.getSnapshotIncludeCollectionList().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(
            props, Map.entry("snapshot.lock.timeout.ms", streamProperties.getSnapshotLockTimeoutMs())),
        assertEntryInProperties(
            props, Map.entry("snapshot.locking.mode", streamProperties.getSnapshotLockingMode())),
        assertEntryInProperties(
            props, Map.entry("snapshot.locking.mode.custom.name", streamProperties.getSnapshotLockingModeCustomName())),
        assertEntryInProperties(props, Map.entry("snapshot.max.threads", streamProperties.getSnapshotMaxThreads())),
        assertEntryInProperties(props, Map.entry("snapshot.mode", streamProperties.getSnapshotMode())),
        assertEntryInProperties(props, Map.entry(
            "snapshot.mode.configuration.based.snapshot.data",
            streamProperties.getSnapshotModeConfigurationBasedSnapshotData())),
        assertEntryInProperties(props, Map.entry(
            "snapshot.mode.configuration.based.snapshot.on.data.error",
            streamProperties.getSnapshotModeConfigurationBasedSnapshotOnDataError())),
        assertEntryInProperties(props, Map.entry(
            "snapshot.mode.configuration.based.snapshot.on.schema.error",
            streamProperties.getSnapshotModeConfigurationBasedSnapshotOnSchemaError())),
        assertEntryInProperties(props, Map.entry(
            "snapshot.mode.configuration.based.snapshot.schema",
            streamProperties.getSnapshotModeConfigurationBasedSnapshotSchema())),
        assertEntryInProperties(props, Map.entry(
            "snapshot.mode.configuration.based.start.stream",
            streamProperties.getSnapshotModeConfigurationBasedStartStream())),
        assertEntryInProperties(
            props, Map.entry("snapshot.mode.custom.name", streamProperties.getSnapshotModeCustomName())),
        assertEntryInProperties(props, Map.entry("snapshot.query.mode", streamProperties.getSnapshotQueryMode())),
        assertEntryInProperties(
            props, Map.entry("snapshot.query.mode.custom.name", streamProperties.getSnapshotQueryModeCustomName())),
        assertEntryInProperties(props, Map.entry(
            "snapshot.select.statement.overrides",
            streamProperties.getSnapshotSelectStatementOverrides().keySet().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "snapshot.select.statement.overrides." + streamProperties.getSnapshotSelectStatementOverrides()
            .keySet().stream().toList().get(0),
            streamProperties.getSnapshotSelectStatementOverrides()
            .values().stream().toList().get(0))),
        assertEntryInProperties(props, Map.entry(
            "snapshot.select.statement.overrides." + streamProperties.getSnapshotSelectStatementOverrides()
            .keySet().stream().toList().get(1),
            streamProperties.getSnapshotSelectStatementOverrides()
            .values().stream().toList().get(1))),
        assertEntryInProperties(
            props, Map.entry("status.update.interval.ms", streamProperties.getStatusUpdateIntervalMs())),
        assertEntryInProperties(props, Map.entry("time.precision.mode", streamProperties.getTimePrecisionMode())),
        assertEntryInProperties(props, Map.entry("tombstones.on.delete", streamProperties.getTombstonesOnDelete())),
        assertEntryInProperties(props, Map.entry("topic.cache.size", streamProperties.getTopicCacheSize())),
        assertEntryInProperties(props, Map.entry("topic.delimiter", streamProperties.getTopicDelimiter())),
        assertEntryInProperties(props, Map.entry("topic.heartbeat.prefix", streamProperties.getTopicHeartbeatPrefix())),
        assertEntryInProperties(props, Map.entry("topic.naming.strategy", streamProperties.getTopicNamingStrategy())),
        assertEntryInProperties(props, Map.entry("topic.transaction", streamProperties.getTopicTransaction())),
        assertEntryInProperties(
            props, Map.entry("unavailable.value.placeholder", streamProperties.getUnavailableValuePlaceholder())),
        assertEntryInProperties(props, Map.entry("xmin.fetch.interval.ms", streamProperties.getXminFetchIntervalMs())),
        assertEntryInProperties(
            props, Map.entry("database.query.timeout.ms", streamProperties.getDatabaseQueryTimeoutMs())),
        assertEntryInProperties(props, Map.entry("read.only", streamProperties.getReadOnly())),
        assertEntryInProperties(
            props, Map.entry("snapshot.isolation.mode", streamProperties.getSnapshotIsolationMode())),
        assertEntryInProperties(props, Map.entry(
            "message.prefix.include.list",
            streamProperties.getMessagePrefixIncludeList().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry(
            "message.prefix.exclude.list",
            streamProperties.getMessagePrefixExcludeList().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry("slot.failover", streamProperties.getSlotFailover())),
        // Leave this so we can order all the properties correctly without bothering for the latest `,`
        Map.entry("|", streamProperties)
        ));
    props.forEach((key, value) -> {
      Assertions.assertTrue(expectedProperties.containsKey(key), key.toString());
    });
  }

  @Test
  void givenAExpectedStream_shouldExtractDebeziumJdbcSinkConnectorCorrectly() {
    StackGresStream stream = ModelTestUtil.createWithRandomData(StackGresStream.class, 2);
    Properties props = new Properties();
    StackGresStreamTargetJdbcSinkDebeziumProperties streamProperties =
        stream.getSpec().getTarget().getSgCluster().getDebeziumProperties();
    DebeziumUtil.configureDebeziumSectionProperties(
        props,
        streamProperties,
        StackGresStreamTargetJdbcSinkDebeziumProperties.class);
    LOGGER.info("Properties for StackGresStreamDebeziumEngineProperties:\n{}",
        props.entrySet().stream()
        .sorted(Comparator.comparing(e -> e.getKey().toString()))
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining("\n")));
    Map<String, Object> expectedProperties = new HashMap<>(Map.ofEntries(
        assertEntryInProperties(props, Map.entry("batch.size", streamProperties.getBatchSize())),
        assertEntryInProperties(props, Map.entry("column.naming.strategy", streamProperties.getColumnNamingStrategy())),
        assertEntryInProperties(props, Map.entry(
            "connection.pool.acquire.increment", streamProperties.getConnectionPoolAcquireIncrement())),
        assertEntryInProperties(props, Map.entry(
            "connection.pool.max.size", streamProperties.getConnectionPoolMaxSize())),
        assertEntryInProperties(props, Map.entry(
            "connection.pool.min.size", streamProperties.getConnectionPoolMinSize())),
        assertEntryInProperties(props, Map.entry(
            "connection.pool.timeout", streamProperties.getConnectionPoolTimeout())),
        assertEntryInProperties(props, Map.entry("database.time.zone", streamProperties.getDatabaseTimeZone())),
        assertEntryInProperties(props, Map.entry("delete.enabled", streamProperties.getDeleteEnabled())),
        assertEntryInProperties(props, Map.entry(
            "dialect.postgres.postgis.schema", streamProperties.getDialectPostgresPostgisSchema())),
        assertEntryInProperties(props, Map.entry(
            "dialect.sqlserver.identity.insert", streamProperties.getDialectSqlserverIdentityInsert())),
        assertEntryInProperties(props, Map.entry("insert.mode", streamProperties.getInsertMode())),
        assertEntryInProperties(props, Map.entry(
            "primary.key.fields",
            streamProperties.getPrimaryKeyFields().stream().collect(Collectors.joining(",")))),
        assertEntryInProperties(props, Map.entry("primary.key.mode", streamProperties.getPrimaryKeyMode())),
        assertEntryInProperties(props, Map.entry("quote.identifiers", streamProperties.getQuoteIdentifiers())),
        assertEntryInProperties(props, Map.entry("schema.evolution", streamProperties.getSchemaEvolution())),
        assertEntryInProperties(props, Map.entry("table.name.format", streamProperties.getTableNameFormat())),
        assertEntryInProperties(props, Map.entry("table.naming.strategy", streamProperties.getTableNamingStrategy())),
        assertEntryInProperties(props, Map.entry("truncate.enabled", streamProperties.getTruncateEnabled())),
        assertEntryInProperties(
            props, Map.entry("connection.url.parameters", streamProperties.getConnectionUrlParameters())),
        assertEntryInProperties(props, Map.entry("use.time.zone", streamProperties.getUseTimeZone())),
        assertEntryInProperties(props, Map.entry("use.reduction.buffer", streamProperties.getUseReductionBuffer())),
        assertEntryInProperties(
            props, Map.entry("collection.naming.strategy", streamProperties.getCollectionNamingStrategy())),
        assertEntryInProperties(props, Map.entry("collection.name.format", streamProperties.getCollectionNameFormat())),
        assertEntryInProperties(props, Map.entry("flush.retry.delay.ms", streamProperties.getFlushRetryDelayMs())),
        assertEntryInProperties(props, Map.entry("flush.max.retries", streamProperties.getFlushMaxRetries())),
        Map.entry("|", streamProperties)
        ));
    props.forEach((key, value) -> {
      Assertions.assertTrue(expectedProperties.containsKey(key), key.toString());
    });
  }

  Map.Entry<String, Object> assertEntryInProperties(Properties props, Map.Entry<String, Object> entry) {
    Assertions.assertTrue(props.containsKey(entry.getKey()), entry.getKey() + " not found");
    Assertions.assertEquals(entry.getValue().toString(), props.get(entry.getKey()).toString(), entry.getKey());
    return entry;
  }

}
