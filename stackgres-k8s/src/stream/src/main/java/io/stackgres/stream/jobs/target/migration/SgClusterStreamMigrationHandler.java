/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.target.migration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import io.debezium.connector.jdbc.JdbcChangeEventSink;
import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.connector.jdbc.QueryBinderResolver;
import io.debezium.connector.jdbc.RecordWriter;
import io.debezium.connector.jdbc.dialect.DatabaseDialect;
import io.debezium.connector.jdbc.dialect.DatabaseDialectResolver;
import io.debezium.connector.jdbc.dialect.postgres.PostgresDatabaseDialect;
import io.debezium.embedded.Connect;
import io.debezium.engine.ChangeEvent;
import io.debezium.pipeline.signal.SignalPayload;
import io.debezium.pipeline.signal.actions.SignalAction;
import io.debezium.pipeline.spi.Partition;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetJdbcSinkDebeziumProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetSgCluster;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.crd.sgstream.StreamTargetType;
import io.stackgres.common.patroni.StackGresPasswordKeys;
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
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.source.SourceRecord;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@StreamTargetOperation(StreamTargetType.SGCLUSTER)
public class SgClusterStreamMigrationHandler implements TargetEventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgClusterStreamMigrationHandler.class);

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

    final StackGresStream stream;

    boolean started = false;
    boolean snapshot = true;
    JdbcChangeEventSink changeEventSink;
    SessionFactory sessionFactory;
    StatelessSession session;
    DatabaseDialect databaseDialect;
    long counter = 0L;
    long lastLsn = 0L;

    JdbcHandler(StackGresStream stream) {
      this.stream = stream;
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
      final String clusterPort = String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT);
      final String clusterDatabase = Optional.ofNullable(stream.getSpec().getSource().getSgCluster())
          .map(StackGresStreamSourceSgCluster::getDatabase)
          .orElse("postgres");
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

      props.setProperty("connection.username", username);
      props.setProperty("connection.password", password);
      props.setProperty("connection.url", "jdbc:postgresql://%s:%s/%s"
          .formatted(
              clusterName,
              clusterPort,
              clusterDatabase));
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
      RecordWriter recordWriter = new RecordWriter(session, queryBinderResolver, config, databaseDialect);

      changeEventSink = new JdbcChangeEventSink(config, session, databaseDialect, recordWriter);

      if (!Optional.ofNullable(stream.getSpec().getTarget()
          .getSgCluster().getSkipDdlImport()).orElse(false)) {
        importDdl(props, namespace, clusterName, clusterPort, clusterDatabase);
      } else {
        LOGGER.info("Import of DDL has been skipped as required by configuration");
      }

      storeAndDropConstraintsAndIndexes();
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
        return new EnhanchedPostgresDatabaseDialect(config, sessionFactory);
      }
      return databaseDialect;
    }

    @Override
    public void consumeEvent(ChangeEvent<SourceRecord, SourceRecord> changeEvent) {
      try {
        if (!started) {
          throw new IllegalStateException("Not started");
        }
        final SourceRecord sourceRecord = changeEvent.value();
        if (snapshot
            && !Optional.ofNullable(sourceRecord.sourceOffset().get("snapshot"))
            .map(Object::toString)
            .map(Boolean.TRUE.toString()::equals)
            .orElse(false)) {
          snapshot = false;
          restoreIndexes();
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
        changeEventSink.execute(List.of(sinkRecord));
        metrics.incrementTotalNumberOfEventsSent();
        metrics.setLastEventSent(sourceOffset);
        metrics.setLastEventWasSent(true);
      } catch (RuntimeException ex) {
        metrics.incrementTotalNumberOfErrorsSeen();
        metrics.setLastEventWasSent(false);
        throw ex;
      } catch (Exception ex) {
        metrics.incrementTotalNumberOfErrorsSeen();
        metrics.setLastEventWasSent(false);
        throw new RuntimeException(ex);
      }
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

    private void importDdl(final Properties props, final String namespace, final String clusterName,
        final String clusterPort, final String clusterDatabase) {
      final String sourceType = stream.getSpec().getSource().getType();
      switch(StreamSourceType.fromString(sourceType)) {
        case SGCLUSTER:
          props.setProperty("connection.url", "jdbc:postgresql://%s:%s/%s"
              .formatted(
                  clusterName,
                  clusterPort,
                  "postgres"));
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
              " host=" + sourceClusterName
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

    private void restoreIndexes() {
      if (Objects.equals(stream.getSpec().getTarget().getType(), StreamTargetType.SGCLUSTER.toString())) {
        restoreIndexesSgCluster();
      }
    }

    private void restoreIndexesSgCluster() {
      LOGGER.info("Restore indexes for target database");
      executeCommand(session, SnapshotHelperQueries.RESTORE_INDEXES.readSql());
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

