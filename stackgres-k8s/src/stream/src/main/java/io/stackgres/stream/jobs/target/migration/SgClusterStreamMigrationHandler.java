/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.target.migration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.debezium.connector.AbstractSourceInfo;
import io.debezium.connector.SnapshotRecord;
import io.debezium.connector.SnapshotType;
import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.QueryBinderResolver;
import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.dialect.DatabaseDialectResolver;
import io.debezium.connector.jdbc.dialect.postgres.PostgresDatabaseDialect;
import io.debezium.connector.postgresql.SourceInfo;
import io.debezium.connector.postgresql.connection.ReplicationMessage.Operation;
import io.debezium.data.Envelope;
import io.debezium.embedded.Connect;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine.RecordCommitter;
import io.debezium.pipeline.signal.SignalPayload;
import io.debezium.pipeline.signal.actions.SignalAction;
import io.debezium.pipeline.spi.Partition;
import io.debezium.relational.RelationalDatabaseConnectorConfig;
import io.debezium.sink.spi.ChangeEventSink;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgres;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgresDebeziumProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetJdbcSinkDebeziumProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetSgCluster;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.crd.sgstream.StreamTargetType;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.stream.jobs.DebeziumUtil;
import io.stackgres.stream.jobs.Metrics;
import io.stackgres.stream.jobs.SourceEventHandler;
import io.stackgres.stream.jobs.StreamDebeziumSignalActionProvider;
import io.stackgres.stream.jobs.StreamTargetOperation;
import io.stackgres.stream.jobs.TargetEventConsumer;
import io.stackgres.stream.jobs.TargetEventHandler;
import io.stackgres.stream.jobs.source.SgClusterDebeziumEngineHandler;
import io.stackgres.stream.jobs.target.migration.postgres.SnapshotHelperQueries;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.ConnectSchema;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@StreamTargetOperation(StreamTargetType.SGCLUSTER)
public class SgClusterStreamMigrationHandler implements TargetEventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgClusterStreamMigrationHandler.class);

  @Inject
  CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  ResourceFinder<Secret> secretFinder;

  @Inject
  Metrics metrics;

  @Override
  public CompletableFuture<Void> sendEvents(StackGresStream stream, SourceEventHandler sourceEventHandler) {
    JdbcHandler handler = null;
    try {
      handler = new JdbcHandler(stream);
      handler.start();
      StreamDebeziumSignalActionProvider.registerSignalAction(
          StreamDebeziumSignalActionProvider.COMMAND_SIGNAL_TYPE, handler);
      return sourceEventHandler.streamChangeEvents(stream, Connect.class, handler);
    } catch (RuntimeException ex) {
      closeHandler(handler, ex);
      throw ex;
    } catch (Exception ex) {
      closeHandler(handler, ex);
      throw new RuntimeException(ex);
    }
  }

  private void closeHandler(JdbcHandler handler, Exception ex) {
    if (handler != null) {
      try {
        handler.close();
      } catch (Exception rex) {
        ex.addSuppressed(rex);
      }
    }
  }

  class JdbcHandler implements TargetEventConsumer<SourceRecord>, SignalAction<Partition> {

    public static final String SNAPSHOT_HEADER_KEY = "stackgres.io/snapshot";
    public static final String INSERT_HEADER_KEY = "stackgres.io/insert";

    final StackGresStream stream;
    final boolean skipDropPrimaryKeys;
    final boolean skipDropIndexes;
    final boolean skipRestoreIndexes;
    final String unavailableValuePlaceholder;
    final String unavailableValuePlaceholderJson;
    final byte[] unavailableValuePlaceholderBytes;
    final byte[] unavailableValuePlaceholderJsonBytes;
    final boolean removePlaceholders;

    boolean started = false;
    boolean snapshot = true;
    ChangeEventSink changeEventSink;
    SessionFactory sessionFactory;
    StatelessSession session;
    DatabaseDialect databaseDialect;
    long counter = 0L;
    long lastLsn = 0L;

    JdbcHandler(StackGresStream stream) {
      this.stream = stream;
      this.skipDropPrimaryKeys = Optional.of(stream.getSpec().getTarget().getSgCluster())
          .map(StackGresStreamTargetSgCluster::getSkipDropPrimaryKeys)
          .orElse(false);
      this.skipDropIndexes = Optional.of(stream.getSpec().getTarget().getSgCluster())
          .map(StackGresStreamTargetSgCluster::getSkipDropIndexesAndConstraints)
          .orElse(false);
      this.skipRestoreIndexes = skipDropIndexes
          || Optional.of(stream.getSpec().getTarget().getSgCluster())
          .map(StackGresStreamTargetSgCluster::getSkipRestoreIndexesAfterSnapshot)
          .orElse(false);
      this.unavailableValuePlaceholder = Optional.ofNullable(stream.getSpec().getSource().getSgCluster())
          .map(StackGresStreamSourceSgCluster::getDebeziumProperties)
          .or(() -> Optional.ofNullable(stream.getSpec().getSource().getPostgres())
              .map(StackGresStreamSourcePostgres::getDebeziumProperties))
          .map(StackGresStreamSourcePostgresDebeziumProperties::getUnavailableValuePlaceholder)
          .orElse(RelationalDatabaseConnectorConfig.DEFAULT_UNAVAILABLE_VALUE_PLACEHOLDER);
      this.unavailableValuePlaceholderJson = fixJsonPlaceholderString(this.unavailableValuePlaceholder);
      this.unavailableValuePlaceholderBytes = this.unavailableValuePlaceholder.getBytes(StandardCharsets.UTF_8);
      this.unavailableValuePlaceholderJsonBytes = fixJsonPlaceholderString(unavailableValuePlaceholder)
          .getBytes(StandardCharsets.UTF_8);
      this.removePlaceholders = Optional.of(stream.getSpec().getTarget().getSgCluster())
          .map(StackGresStreamTargetSgCluster::getDebeziumProperties)
          .map(StackGresStreamTargetJdbcSinkDebeziumProperties::getRemovePlaceholders)
          .orElse(false);
      if (skipRestoreIndexes) {
        snapshot = false;
      }
    }

    public void start() {
      if (started) {
        throw new IllegalStateException("Already started");
      }
      started = true;
      final Properties props = new Properties();
      final var sgCluster = Optional.of(stream.getSpec().getTarget().getSgCluster());
      sgCluster
          .map(StackGresStreamTargetSgCluster::getDebeziumProperties)
          .map(StackGresStreamTargetJdbcSinkDebeziumProperties::getPrimaryKeyMode)
          .filter("kafka"::equalsIgnoreCase)
          .ifPresent(mode -> {
            throw new IllegalArgumentException("primaryKeyMode kafka is not supported");
          });
      final String namespace = stream.getMetadata().getNamespace();
      props.setProperty("name", SgClusterDebeziumEngineHandler.name(stream));
      props.setProperty("topic", SgClusterDebeziumEngineHandler.name(stream));
      DebeziumUtil.configureDebeziumSectionProperties(
          props,
          sgCluster
          .map(StackGresStreamTargetSgCluster::getDebeziumProperties)
          .orElse(null),
          StackGresStreamTargetJdbcSinkDebeziumProperties.class);
      final String clusterName = sgCluster.map(StackGresStreamTargetSgCluster::getName)
          .orElseThrow(() -> new IllegalArgumentException("The name of SGCluster is not specified"));
      final StackGresCluster cluster = clusterFinder.findByNameAndNamespace(clusterName, namespace)
          .orElseThrow(() -> new IllegalArgumentException(StackGresCluster.KIND + " " + clusterName + " not found"));
      final String clusterServiceName = PatroniUtil.readWriteName(cluster);
      final String clusterPort = String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT);
      final String clusterDatabase = Optional.ofNullable(stream.getSpec().getTarget().getSgCluster())
          .map(StackGresStreamTargetSgCluster::getDatabase)
          .orElse("postgres");
      final String clusterParameters = Optional.ofNullable(stream.getSpec().getTarget().getSgCluster())
          .map(StackGresStreamTargetSgCluster::getDebeziumProperties)
          .map(StackGresStreamTargetJdbcSinkDebeziumProperties::getConnectionUrlParameters)
          .orElse("");
      final String usernameSecretName = sgCluster
          .map(StackGresStreamTargetSgCluster::getUsername)
          .map(SecretKeySelector::getName)
          .orElseGet(() -> PatroniUtil.secretName(clusterName));
      final String usernameSecretKey = sgCluster
          .map(StackGresStreamTargetSgCluster::getUsername)
          .map(SecretKeySelector::getKey)
          .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_USERNAME_KEY);
      final var username = getSecretKeyValue(namespace, usernameSecretName, usernameSecretKey);
      final String passwordSecretName = sgCluster
          .map(StackGresStreamTargetSgCluster::getPassword)
          .map(SecretKeySelector::getName)
          .orElseGet(() -> PatroniUtil.secretName(clusterName));
      final String passwordSecretKey = sgCluster
          .map(StackGresStreamTargetSgCluster::getPassword)
          .map(SecretKeySelector::getKey)
          .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY);
      final var password = getSecretKeyValue(namespace, passwordSecretName, passwordSecretKey);
      final boolean detectIsertMode = sgCluster
          .map(StackGresStreamTargetSgCluster::getDebeziumProperties)
          .map(StackGresStreamTargetJdbcSinkDebeziumProperties::getDetectInsertMode)
          .orElse(true);

      props.setProperty("connection.username", username);
      props.setProperty("connection.password", password);
      props.setProperty("connection.url", "jdbc:postgresql://%s:%s/%s?%s"
          .formatted(
              clusterServiceName,
              clusterPort,
              clusterDatabase,
              clusterParameters));
      final JdbcSinkConnectorConfig config = new JdbcSinkConnectorConfig(props
          .entrySet()
          .stream()
          .map(e -> Map.entry(e.getKey().toString(), e.getValue().toString()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
      config.validate();

      // Sync the code below with the code of the method
      // io.debezium.connector.jdbc.JdbcSinkConnectorTask.start(java.util.Map<String, String>)
      sessionFactory = config.getHibernateConfiguration().buildSessionFactory();
      session = sessionFactory.openStatelessSession();
      databaseDialect = resolveDatabaseDialect(config, sessionFactory);
      QueryBinderResolver queryBinderResolver = new QueryBinderResolver();
      EnhancedRecordWriter recordWriter =
          new EnhancedRecordWriter(session, queryBinderResolver, config, databaseDialect, this, detectIsertMode);

      changeEventSink = new EnhancedJdbcChangeEventSink(
          config, session, databaseDialect, recordWriter);

      if (!Optional.ofNullable(stream.getSpec().getTarget()
          .getSgCluster().getSkipDdlImport()).orElse(false)) {
        importDdl(props, namespace, clusterServiceName, clusterPort, clusterDatabase, clusterParameters);
      } else {
        LOGGER.info("Import of DDL has been skipped as required by configuration");
      }

      if (!skipDropIndexes) {
        storeAndDropConstraintsAndIndexes();
      } else {
        LOGGER.info("Skipping storing and removing constraints and indexes for target database");
      }

      if (!skipDropPrimaryKeys) {
        storeAndDropPrimaryKeys();
      } else {
        LOGGER.info("Skipping storing and removing primary keys for target database");
      }
    }

    @Override
    public void close() throws Exception {
      if (changeEventSink != null) {
        changeEventSink.close();
      }
      if (session != null && session.isOpen()) {
        session.close();
      }
      if (sessionFactory != null && sessionFactory.isOpen()) {
        sessionFactory.close();
      }
    }

    private DatabaseDialect resolveDatabaseDialect(JdbcSinkConnectorConfig config, SessionFactory sessionFactory) {
      final DatabaseDialect databaseDialect = DatabaseDialectResolver.resolve(config, sessionFactory);
      if (databaseDialect instanceof PostgresDatabaseDialect) {
        return new EnhancedPostgresDatabaseDialect(this, config, sessionFactory);
      }
      return databaseDialect;
    }

    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION",
        justification = "wanted behavior")
    @Override
    public void consumeEvents(
        List<ChangeEvent<SourceRecord, SourceRecord>> changeEvents,
        RecordCommitter<ChangeEvent<SourceRecord, SourceRecord>> committer) {
      final List<SinkRecord> sinkRecords = new ArrayList<>(changeEvents.size());
      try {
        if (!started) {
          throw new IllegalStateException("Not started");
        }
        if (changeEvents.isEmpty()) {
          Unchecked.runnable(() -> committer.markBatchFinished()).run();
          return;
        }
        final Iterator<ChangeEvent<SourceRecord, SourceRecord>> changeEventIterator = changeEvents.iterator();
        final List<ChangeEvent<SourceRecord, SourceRecord>> committedChangeEvents = new ArrayList<>(changeEvents.size());
        String lastSourceOffset = null;
        while (changeEventIterator.hasNext()) {
          ChangeEvent<SourceRecord, SourceRecord> changeEvent = changeEventIterator.next();
          final SourceRecord originalSourceRecord = changeEvent.value();
          final SourceRecord sourceRecord;
          if (removePlaceholders) {
            sourceRecord = addInsertModeHintsHeaders(removePlaceholderValues(originalSourceRecord));
          } else {
            sourceRecord = addInsertModeHintsHeaders(fixPlaceholderValues(originalSourceRecord));
          }
          if (snapshot && !isSnapshot(sourceRecord)) {
            snapshot = false;
            if (!sinkRecords.isEmpty()) {
              changeEventSink.execute(sinkRecords);
              for (var committedChangeEvent : committedChangeEvents) {
                Unchecked.runnable(() -> committer.markProcessed(committedChangeEvent)).run();
              }
              metrics.incrementTotalNumberOfEventsSent(sinkRecords.size());
              metrics.setLastEventSent(lastSourceOffset);
              metrics.setLastEventWasSent(true);
            }
            sinkRecords.clear();
            committedChangeEvents.clear();
            if (!skipDropPrimaryKeys) {
              LOGGER.info("Restoring primary keys for target database");
              restorePrimaryKeys();
            } else {
              LOGGER.info("Skipping restoring primary keys for target database");
            }
            if (!skipRestoreIndexes) {
              LOGGER.info("Restoring indexes for target database");
              restoreIndexes();
            } else {
              LOGGER.info("Skipping restoring indexes for target database");
            }
          }
          String sourceOffset = sourceRecord.sourceOffset()
              .entrySet()
              .stream()
              .map(e -> e.getKey() + "=" + e.getValue().toString())
              .collect(Collectors.joining(" "));
          LOGGER.trace("SourceRecord: {}", sourceOffset);
          long lsn = Long.parseLong(sourceRecord.sourceOffset().get("lsn").toString());
          if (lastLsn != lsn) {
            lastLsn = lsn;
            counter = 0L;
          }
          long kafkaPartition = (lastLsn << 32) & (counter & ((1 << 32) - 1));
          counter++;
          SinkRecord sinkRecord = new SinkRecord(
              sourceRecord.topic(),
              Optional.ofNullable(changeEvent.partition()).orElse(0).intValue(),
              sourceRecord.keySchema(),
              sourceRecord.key(),
              sourceRecord.valueSchema(),
              sourceRecord.value(),
              kafkaPartition,
              sourceRecord.timestamp(),
              TimestampType.CREATE_TIME,
              sourceRecord.headers());
          sinkRecords.add(sinkRecord);
          committedChangeEvents.add(changeEvent);
          lastSourceOffset = sourceOffset;
        }
        changeEventSink.execute(sinkRecords);
        for (var committedChangeEvent : committedChangeEvents) {
          Unchecked.runnable(() -> committer.markProcessed(committedChangeEvent)).run();
        }
        Unchecked.runnable(() -> committer.markBatchFinished()).run();
        metrics.incrementTotalNumberOfEventsSent(sinkRecords.size());
        metrics.setLastEventSent(lastSourceOffset);
        metrics.setLastEventWasSent(true);
      } catch (Exception ex) {
        metrics.incrementTotalNumberOfErrorsSeen();
        metrics.setLastEventWasSent(false);
        throw new RuntimeException(
            "Error while processing topics "
                + sinkRecords.stream()
                .map(SinkRecord::topic)
                .collect(Collectors.groupingBy(Function.identity()))
                .keySet()
                .stream()
                .collect(Collectors.joining(", ")),
            ex);
      }
    }

    private final static List<String> SNAPSHOT_VALUES =
        Seq.<String>of()
        .append(Seq.of(SnapshotRecord.values()).filter(Predicate.not(SnapshotRecord.FALSE::equals)).map(Enum::name))
        .append(Seq.of(SnapshotType.values()).map(Enum::name))
        .toList();

    private boolean isSnapshot(final SourceRecord sourceRecord) {
      return Optional.ofNullable(sourceRecord.sourceOffset().get(AbstractSourceInfo.SNAPSHOT_KEY))
      .map(Object::toString)
      .filter(SNAPSHOT_VALUES::contains)
      .map(snapshot -> true)
      .orElse(false);
    }

    private SourceRecord addInsertModeHintsHeaders(final SourceRecord sourceRecord) {
      final ConnectHeaders newHeaders = new ConnectHeaders(sourceRecord.headers());
      final boolean isSnapshot = isSnapshot(sourceRecord);
      final boolean isInsert = Objects.equals(
          sourceRecord.sourceOffset().get(SourceInfo.MSG_TYPE_KEY),
          Operation.INSERT.name());
      if (isSnapshot || isInsert) {
        if (isSnapshot) {
          newHeaders.add(SNAPSHOT_HEADER_KEY, true, Schema.BOOLEAN_SCHEMA);
        }
        newHeaders.add(INSERT_HEADER_KEY, true, Schema.BOOLEAN_SCHEMA);
        return new SourceRecord(
            sourceRecord.sourcePartition(),
            sourceRecord.sourceOffset(),
            sourceRecord.topic(),
            sourceRecord.kafkaPartition(),
            sourceRecord.keySchema(),
            sourceRecord.key(),
            sourceRecord.valueSchema(),
            sourceRecord.value(),
            sourceRecord.timestamp(),
            newHeaders);
      }
      return sourceRecord;
    }

    private SourceRecord removePlaceholderValues(final SourceRecord sourceRecord) {
      if (sourceRecord.value() != null
          && sourceRecord.value() instanceof Struct originalValue) {
        final boolean isDebeziumMessage = originalValue != null
            && sourceRecord.valueSchema().name() != null
            && sourceRecord.valueSchema().name().contains("Envelope")
            && originalValue.getStruct(Envelope.FieldName.AFTER) != null;
        final Struct candidateValue;
        if (isDebeziumMessage) {
          candidateValue = originalValue.getStruct(Envelope.FieldName.AFTER);
        } else {
          candidateValue = originalValue;
        }
        if (candidateValue.schema().fields().stream()
            .anyMatch(field -> isPlaceholder(candidateValue.get(field)))) {
          final List<Field> valueFields = new ArrayList<Field>(
              (int) candidateValue.schema().fields().stream()
              .filter(field -> !isPlaceholder(candidateValue.get(field)))
              .count());
          {
            int index = 0;
            for (Field field : candidateValue.schema().fields()) {
              if (isPlaceholder(candidateValue.get(field))) {
                continue;
              }
              valueFields.add(new Field(field.name(), index, field.schema()));
              index++;
            }
          }
          final ConnectSchema valueSchema = new ConnectSchema(
              candidateValue.schema().type(),
              candidateValue.schema().isOptional(),
              candidateValue.schema().defaultValue(),
              candidateValue.schema().name(),
              candidateValue.schema().version(),
              candidateValue.schema().doc(),
              candidateValue.schema().parameters(),
              valueFields,
              null,
              null);
          final Struct value = new Struct(valueSchema);
          for (Field field : valueFields) {
            value.put(field, candidateValue.get(field.name()));
          }
          if (isDebeziumMessage) {
            List<Field> newFields = new ArrayList<>(
                originalValue.schema().fields());
            for (int index = 0; index < newFields.size(); index++) {
              if (Objects.equals(newFields.get(index).name(), Envelope.FieldName.AFTER)) {
                newFields.set(index, new Field(
                    Envelope.FieldName.AFTER,
                    originalValue.schema().field(Envelope.FieldName.AFTER).index(),
                    valueSchema));
              }
            }
            ConnectSchema newSchema = new ConnectSchema(
                originalValue.schema().type(),
                originalValue.schema().isOptional(),
                originalValue.schema().defaultValue(),
                originalValue.schema().name(),
                originalValue.schema().version(),
                originalValue.schema().doc(),
                originalValue.schema().parameters(),
                newFields,
                null,
                null);
            Struct newValue = new Struct(newSchema);
            for (int index = 0; index < newFields.size(); index++) {
              if (Objects.equals(newFields.get(index).name(), Envelope.FieldName.AFTER)) {
                newValue.put(newFields.get(index), value);
              } else {
                newValue.put(newFields.get(index), originalValue.get(newFields.get(index).name()));
              }
            }
            return new SourceRecord(
                sourceRecord.sourcePartition(),
                sourceRecord.sourceOffset(),
                sourceRecord.topic(),
                sourceRecord.kafkaPartition(),
                sourceRecord.keySchema(),
                sourceRecord.key(),
                newSchema,
                newValue,
                sourceRecord.timestamp(),
                sourceRecord.headers());
          } else {
            return new SourceRecord(
                sourceRecord.sourcePartition(),
                sourceRecord.sourceOffset(),
                sourceRecord.topic(),
                sourceRecord.kafkaPartition(),
                sourceRecord.keySchema(),
                sourceRecord.key(),
                valueSchema,
                value,
                sourceRecord.timestamp(),
                sourceRecord.headers());
          }
        }
      }
      return sourceRecord;
    }

    private SourceRecord fixPlaceholderValues(final SourceRecord sourceRecord) {
      if (sourceRecord.value() != null
          && sourceRecord.value() instanceof Struct originalValue) {
        final boolean isDebeziumMessage = originalValue != null
            && sourceRecord.valueSchema().name() != null
            && sourceRecord.valueSchema().name().contains("Envelope")
            && originalValue.getStruct(Envelope.FieldName.AFTER) != null;
        final Struct candidateValue;
        if (isDebeziumMessage) {
          candidateValue = originalValue.getStruct(Envelope.FieldName.AFTER);
        } else {
          candidateValue = originalValue;
        }
        if (candidateValue.schema().fields().stream()
            .anyMatch(field -> isJsonPlaceholder(field, candidateValue.get(field)))) {
          final Struct value = new Struct(candidateValue.schema());
          for (Field field : candidateValue.schema().fields()) {
            final Object currentValue = candidateValue.get(field.name());
            if (isJsonPlaceholder(field, currentValue)) {
              value.put(field, fixJsonPlaceholder(currentValue));
            } else {
              value.put(field, currentValue);
            }
          }
          if (isDebeziumMessage) {
            List<Field> newFields = new ArrayList<>(
                originalValue.schema().fields());
            for (int index = 0; index < newFields.size(); index++) {
              if (Objects.equals(newFields.get(index).name(), Envelope.FieldName.AFTER)) {
                newFields.set(index, new Field(
                    Envelope.FieldName.AFTER,
                    originalValue.schema().field(Envelope.FieldName.AFTER).index(),
                    candidateValue.schema()));
              }
            }
            ConnectSchema newSchema = new ConnectSchema(
                originalValue.schema().type(),
                originalValue.schema().isOptional(),
                originalValue.schema().defaultValue(),
                originalValue.schema().name(),
                originalValue.schema().version(),
                originalValue.schema().doc(),
                originalValue.schema().parameters(),
                newFields,
                null,
                null);
            Struct newValue = new Struct(newSchema);
            for (int index = 0; index < newFields.size(); index++) {
              if (Objects.equals(newFields.get(index).name(), Envelope.FieldName.AFTER)) {
                newValue.put(newFields.get(index), value);
              } else {
                newValue.put(newFields.get(index), originalValue.get(newFields.get(index).name()));
              }
            }
            return new SourceRecord(
                sourceRecord.sourcePartition(),
                sourceRecord.sourceOffset(),
                sourceRecord.topic(),
                sourceRecord.kafkaPartition(),
                sourceRecord.keySchema(),
                sourceRecord.key(),
                newSchema,
                newValue,
                sourceRecord.timestamp(),
                sourceRecord.headers());
          } else {
            final ConnectSchema valueSchema = new ConnectSchema(
                candidateValue.schema().type(),
                candidateValue.schema().isOptional(),
                candidateValue.schema().defaultValue(),
                candidateValue.schema().name(),
                candidateValue.schema().version(),
                candidateValue.schema().doc(),
                candidateValue.schema().parameters(),
                candidateValue.schema().fields(),
                null,
                null);
            return new SourceRecord(
                sourceRecord.sourcePartition(),
                sourceRecord.sourceOffset(),
                sourceRecord.topic(),
                sourceRecord.kafkaPartition(),
                sourceRecord.keySchema(),
                sourceRecord.key(),
                valueSchema,
                value,
                sourceRecord.timestamp(),
                sourceRecord.headers());
          }
        }
      }
      return sourceRecord;
    }

    private Object fixJsonPlaceholder(Object value) {
      if (value instanceof List<?> valueList) {
        return valueList.stream()
            .map(this::fixJsonPlaceholder)
            .toList();
      }
      if (value instanceof byte[] currentValueBytes) {
        return fixJsonPlaceholderString(new String(currentValueBytes, StandardCharsets.UTF_8))
            .getBytes(StandardCharsets.UTF_8);
      }
      return fixJsonPlaceholderString(value.toString());
    }

    private String fixJsonPlaceholderString(String value) {
      return '"' + value + '"';
    }

    private boolean isJsonPlaceholder(Field field, Object value) {
      if (field.schema().parameters() == null) {
        return false;
      }
      final String fieldType = field.schema().parameters().get("__debezium.source.column.type")
          .toLowerCase(Locale.US);
      return (Objects.equals(fieldType, "json") || Objects.equals(fieldType, "_json")
          || Objects.equals(fieldType, "jsonb") || Objects.equals(fieldType, "_jsonb")
          || Objects.equals(fieldType, "jsonpath") || Objects.equals(fieldType, "_jsonpath"))
          && isPlaceholder(value);
    }

    public boolean isPlaceholder(Object value) {
      return Objects.equals(value, unavailableValuePlaceholder)
          || Objects.equals(value, unavailableValuePlaceholderJson)
          || Objects.deepEquals(value, unavailableValuePlaceholderBytes)
          || Objects.deepEquals(value, unavailableValuePlaceholderJsonBytes)
          || (value instanceof List<?> valueList
              && (isValueListPlaceholderBytes(valueList, unavailableValuePlaceholderBytes)
                  || isValueListPlaceholderBytes(valueList, unavailableValuePlaceholderJsonBytes)
                  || (valueList.size() == 1
                  && isPlaceholder(valueList.get(0)))));
    }

    private boolean isValueListPlaceholderBytes(List<?> valueList, byte[] placeholderBytes) {
      return valueList.size() == placeholderBytes.length
          && IntStream.range(0, placeholderBytes.length)
          .allMatch(index -> valueList.get(index) instanceof Number valueElementNumber
              && ((valueElementNumber instanceof Integer valueElementInteger
                  && placeholderBytes[index] == valueElementInteger.byteValue())
                  || (valueElementNumber instanceof Long valueElementLong
                      && placeholderBytes[index] == valueElementLong.byteValue())
                  || (valueElementNumber instanceof Float valueElementFloat
                      && placeholderBytes[index] == valueElementFloat.byteValue())
                  || (valueElementNumber instanceof Double valueElementDouble
                      && placeholderBytes[index] == valueElementDouble.byteValue())
                  ));
    }

    @Override
    public boolean arrived(SignalPayload<Partition> signalPayload) throws InterruptedException {
      if (StreamDebeziumSignalActionProvider.COMMAND_SIGNAL_TYPE.equals(signalPayload.type)) {
        final String commandSql = signalPayload.data.getString("command");
        LOGGER.trace("SQL: {}", commandSql);
  
        executeCommand(session, commandSql);

        return true;
      }

      return false;
    }

    private List<Object> executeQuery(StatelessSession session, String commandSql) {
      Transaction transaction = session.beginTransaction();
      try {
        List<Object> result = session.createNativeQuery(commandSql, Object.class).getResultList();
        transaction.commit();
        return result;
      } catch (RuntimeException ex) {
        transaction.rollback();
        throw ex;
      } catch (Exception ex) {
        transaction.rollback();
        throw new RuntimeException(ex);
      }
    }

    private void executeCommand(StatelessSession session, String commandSql) {
      Transaction transaction = session.beginTransaction();
      try {
        session.createNativeQuery(commandSql, Object.class).executeUpdate();
        transaction.commit();
      } catch (RuntimeException ex) {
        transaction.rollback();
        throw ex;
      } catch (Exception ex) {
        transaction.rollback();
        throw new RuntimeException(ex);
      }
    }

    private void importDdl(
        final Properties props,
        final String namespace,
        final String clusterServiceName,
        final String clusterPort,
        final String clusterDatabase,
        final String clusterParameters) {
      final String sourceType = stream.getSpec().getSource().getType();
      switch(StreamSourceType.fromString(sourceType)) {
        case SGCLUSTER:
          props.setProperty("connection.url", "jdbc:postgresql://%s:%s/%s?%s"
              .formatted(
                  clusterServiceName,
                  clusterPort,
                  "postgres",
                  clusterParameters));
          final JdbcSinkConnectorConfig importConfig = new JdbcSinkConnectorConfig(props
              .entrySet()
              .stream()
              .map(e -> Map.entry(e.getKey().toString(), e.getValue().toString()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
          try (var sessionFactory = importConfig.getHibernateConfiguration().buildSessionFactory();
              var session = sessionFactory.openStatelessSession()) {
            importDdlSgCluster(session, namespace, clusterDatabase);
          }
          break;
        default:
          LOGGER.info("Can not import DDL from source type {}", sourceType);
          break;
      }
    }

    private void importDdlSgCluster(StatelessSession session, String namespace, String clusterDatabase) {
      var sourceSgCluster = Optional.of(stream.getSpec().getSource().getSgCluster());
      String sourceClusterPort = String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT);
      String sourceClusterDatabase = Optional.ofNullable(stream.getSpec().getSource().getSgCluster())
          .map(StackGresStreamSourceSgCluster::getDatabase)
          .orElse("postgres");
      String sourceClusterName = sourceSgCluster.map(StackGresStreamSourceSgCluster::getName)
          .orElseThrow(() -> new IllegalArgumentException("The name of SGCluster is not specified"));
      final StackGresCluster sourceCluster = clusterFinder.findByNameAndNamespace(sourceClusterName, namespace)
          .orElseThrow(() -> new IllegalArgumentException(StackGresCluster.KIND + " " + sourceClusterName + " not found"));
      final String sourceClusterServiceName = PatroniUtil.readWriteName(sourceCluster);
      String sourceUsernameSecretName = sourceSgCluster
          .map(StackGresStreamSourceSgCluster::getUsername)
          .map(SecretKeySelector::getName)
          .orElseGet(() -> PatroniUtil.secretName(sourceClusterName));
      String sourceUsernameSecretKey = sourceSgCluster
          .map(StackGresStreamSourceSgCluster::getUsername)
          .map(SecretKeySelector::getKey)
          .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_USERNAME_KEY);
      var sourceUsername = getSecretKeyValue(namespace, sourceUsernameSecretName, sourceUsernameSecretKey);
      String sourcePasswordSecretName = sourceSgCluster
          .map(StackGresStreamSourceSgCluster::getPassword)
          .map(SecretKeySelector::getName)
          .orElseGet(() -> PatroniUtil.secretName(sourceClusterName));
      String sourcePasswordSecretKey = sourceSgCluster
          .map(StackGresStreamSourceSgCluster::getPassword)
          .map(SecretKeySelector::getKey)
          .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY);
      var sourcePassword = getSecretKeyValue(namespace, sourcePasswordSecretName, sourcePasswordSecretKey);
      var sourceSuperuserUsername = getSecretKeyValue(namespace, PatroniUtil.secretName(sourceClusterName), StackGresPasswordKeys.SUPERUSER_USERNAME_KEY);
      var sourceReplicationUsername = getSecretKeyValue(namespace, PatroniUtil.secretName(sourceClusterName), StackGresPasswordKeys.REPLICATION_USERNAME_KEY);
      var sourceAuthenticatorUsername = getSecretKeyValue(namespace, PatroniUtil.secretName(sourceClusterName), StackGresPasswordKeys.AUTHENTICATOR_USERNAME_KEY);

      LOGGER.info("Importing DDL from source SGCluster {} for database {}", sourceClusterName, sourceClusterDatabase);
      executeCommand(session, SnapshotHelperQueries.IMPORT_DDL.readSql().formatted(
              " host=" + sourceClusterServiceName
              + " port=" + sourceClusterPort
              + " dbname=" + sourceClusterDatabase
              + " user=" + sourceUsername
              + " password=" + sourcePassword,
              sourceClusterDatabase,
              clusterDatabase,
              Optional.ofNullable(stream.getSpec().getTarget()
                  .getSgCluster().getDdlImportRoleSkipFilter())
              .orElse("(" + sourceSuperuserUsername
                  + "|" + sourceReplicationUsername
                  + "|" + sourceAuthenticatorUsername + ")")));
    }

    private void storeAndDropPrimaryKeys() {
      if (Objects.equals(stream.getSpec().getTarget().getType(), StreamTargetType.SGCLUSTER.toString())) {
        storeAndDropPrimaryKeysSgCluster();
      }
    }

    private void storeAndDropPrimaryKeysSgCluster() {
      LOGGER.info("Store primary keys for target database");
      executeCommand(session, SnapshotHelperQueries.STORE_PRIMARY_KEYS.readSql());
      LOGGER.info("Drop primary keys for target database");
      executeCommand(session, SnapshotHelperQueries.DROP_PRIMARY_KEYS.readSql());
    }

    private void storeAndDropConstraintsAndIndexes() {
      if (Objects.equals(stream.getSpec().getTarget().getType(), StreamTargetType.SGCLUSTER.toString())) {
        storeAndDropConstraintsAndIndexesSgCluster();
      }
    }

    private void storeAndDropConstraintsAndIndexesSgCluster() {
      LOGGER.info("Store constraints for target database");
      executeCommand(session, SnapshotHelperQueries.STORE_CONSTRAINTS.readSql());
      LOGGER.info("Store indexes for target database");
      executeCommand(session, SnapshotHelperQueries.STORE_INDEXES.readSql());
      LOGGER.info("Drop constraints for target database");
      executeCommand(session, SnapshotHelperQueries.DROP_CONSTRAINTS.readSql());
      LOGGER.info("Drop indexes for target database");
      executeCommand(session, SnapshotHelperQueries.DROP_INDEXES.readSql());
    }

    private void restorePrimaryKeys() {
      if (Objects.equals(stream.getSpec().getTarget().getType(), StreamTargetType.SGCLUSTER.toString())) {
        restorePrimaryKeysSgCluster();
      }
    }

    private void restorePrimaryKeysSgCluster() {
      if (Objects.equals(stream.getSpec().getTarget().getType(), StreamTargetType.SGCLUSTER.toString())) {
        LOGGER.info("Restore primary keys for target database");
        var result = executeQuery(session, SnapshotHelperQueries.CHECK_RESTORE_PRIMARY_KEYS.readSql());
        if (result == null || result.size() <= 0 || !(result.get(0) instanceof Number)) {
          throw new RuntimeException("Undefined result while restoring objects on target database");
        }
        final int resultCount = Number.class.cast(result.get(0)).intValue();
        for (int index = 0; index < resultCount; index++) {
          executeCommand(session, SnapshotHelperQueries.RESTORE_PRIMARY_KEYS.readSql());
        }
      }
    }

    private void restoreIndexes() {
      if (Objects.equals(stream.getSpec().getTarget().getType(), StreamTargetType.SGCLUSTER.toString())) {
        restoreIndexesSgCluster();
      }
    }

    private void restoreIndexesSgCluster() {
      LOGGER.info("Restore indexes for target database");
      var result = executeQuery(session, SnapshotHelperQueries.CHECK_RESTORE_INDEXES.readSql());
      if (result == null || result.size() <= 0 || !(result.get(0) instanceof Number)) {
        throw new RuntimeException("Undefined result while restoring objects on target database");
      }
      final int resultCount = Number.class.cast(result.get(0)).intValue();
      for (int index = 0; index < resultCount; index++) {
        executeCommand(session, SnapshotHelperQueries.RESTORE_INDEXES.readSql());
      }
    }
  }

  private String getSecretKeyValue(String namespace, String secretName, String secretKey) {
    return Optional.of(secretFinder.findByNameAndNamespace(secretName, namespace)
        .orElseThrow(() -> new IllegalArgumentException("Secret " + secretName + " not found")))
        .map(Secret::getData)
        .map(data -> data.get(secretKey))
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new IllegalArgumentException("key " + secretKey + " not found in Secret " + secretName));
  }

}

