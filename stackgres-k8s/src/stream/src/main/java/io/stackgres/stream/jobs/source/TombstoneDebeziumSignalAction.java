/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.source;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.debezium.connector.jdbc.JdbcSinkConnectorConfig;
import io.debezium.engine.DebeziumEngine;
import io.debezium.pipeline.signal.SignalPayload;
import io.debezium.pipeline.signal.actions.SignalAction;
import io.debezium.pipeline.spi.Partition;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgres;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import io.stackgres.common.crd.sgstream.StackGresStreamTargetSgCluster;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.crd.sgstream.StreamStatusCondition;
import io.stackgres.common.crd.sgstream.StreamTargetType;
import io.stackgres.common.kubernetesclient.KubernetesClientUtil;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.stream.jobs.StreamDebeziumSignalActionProvider;
import io.stackgres.stream.jobs.target.migration.postgres.SnapshotHelperQueries;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TombstoneDebeziumSignalAction implements SignalAction<Partition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TombstoneDebeziumSignalAction.class);

  final StackGresStream stream;
  final CustomResourceScheduler<StackGresStream> streamScheduler;
  final CustomResourceFinder<StackGresCluster> clusterFinder;
  final ResourceFinder<Secret> secretFinder;
  final DebeziumEngine<?> engine;
  final CompletableFuture<Void> streamCompleted;
  volatile CompletableFuture<Void> tombstoneCompleted;

  public TombstoneDebeziumSignalAction(
      StackGresStream stream,
      CustomResourceScheduler<StackGresStream> streamScheduler,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      ResourceFinder<Secret> secretFinder,
      DebeziumEngine<?> engine,
      CompletableFuture<Void> streamCompleted) {
    this.stream = stream;
    this.streamScheduler = streamScheduler;
    this.clusterFinder = clusterFinder;
    this.secretFinder = secretFinder;
    this.engine = engine;
    this.streamCompleted = streamCompleted;
  }

  @Override
  public boolean arrived(SignalPayload<Partition> signalPayload) throws InterruptedException {
    if (StreamDebeziumSignalActionProvider.TOMBSTONE_SIGNAL_TYPE.equals(signalPayload.type)) {
      handleTombstone();

      return true;
    }

    return false;
  }

  public synchronized CompletableFuture<Void> getTombstoneCompleted() {
    return tombstoneCompleted != null ? tombstoneCompleted : CompletableFuture.completedFuture(null);
  }

  private synchronized void handleTombstone() {
    if (streamCompleted.isDone() || tombstoneCompleted != null) {
      throw new IllegalStateException(
          "Tombsone singal can not be handled when stream is already closed or closing");
    }
    tombstoneCompleted = new CompletableFuture<>();
    CompletableFuture
        .runAsync(Unchecked.runnable(engine::close))
        .handleAsync(Unchecked.biFunction((ignore, ex) -> {
          if (ex != null) {
            LOGGER.error("An error occurred while stopping Debezium engine on tombstone signal", ex);
            throw ex;
          }
          return ignore;
        }))
        .thenRunAsync(this::restoreTargetConstraints)
        .thenRunAsync(this::cleanupSource)
        .thenRunAsync(() -> KubernetesClientUtil.retryOnConflict(() -> streamScheduler
            .update(stream, this::setCompletedStatusCondition)))
        .handleAsync((ignore, ex) -> {
          if (ex != null) {
            tombstoneCompleted.completeExceptionally(ex);
          } else {
            tombstoneCompleted.complete(null);
          }
          return null;
        });
  }

  private void setCompletedStatusCondition(StackGresStream currentStream) {
    if (currentStream.getStatus() == null) {
      currentStream.setStatus(new StackGresStreamStatus());
    }
    final List<Condition> conditions = List.of(
        StreamStatusCondition.STREAM_FALSE_RUNNING.getCondition(),
        StreamStatusCondition.STREAM_COMPLETED.getCondition(),
        StreamStatusCondition.STREAM_FALSE_FAILED.getCondition()
    );
    Condition.setTransitionTimes(conditions);
    currentStream.getStatus().setConditions(conditions);
  }

  private void restoreTargetConstraints() {
    if (Objects.equals(stream.getSpec().getTarget().getType(), StreamTargetType.SGCLUSTER.toString())) {
      if (Optional.of(stream.getSpec().getTarget().getSgCluster())
          .map(StackGresStreamTargetSgCluster::getSkipDropIndexesAndConstraints)
          .orElse(false)) {
        LOGGER.info("Skipping restoring constraints and indexes for target database on tombstone signal");
        return;
      }
      final Properties props = new Properties();
      final var sgCluster = Optional.of(stream.getSpec().getTarget().getSgCluster());
      final String namespace = stream.getMetadata().getNamespace();
      final String clusterName = sgCluster.map(StackGresStreamTargetSgCluster::getName)
          .orElseThrow(() -> new IllegalArgumentException("The name of SGCluster is not specified"));
      final StackGresCluster cluster = clusterFinder.findByNameAndNamespace(clusterName, namespace)
          .orElseThrow(() -> new IllegalArgumentException(StackGresCluster.KIND + " " + clusterName + " not found"));
      final String clusterServiceName = PatroniUtil.readWriteName(cluster);
      final String clusterPort = String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT);
      final String clusterDatabase = Optional.ofNullable(stream.getSpec().getTarget().getSgCluster())
          .map(StackGresStreamTargetSgCluster::getDatabase)
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
              clusterServiceName,
              clusterPort,
              clusterDatabase));
      final JdbcSinkConnectorConfig config = new JdbcSinkConnectorConfig(props
          .entrySet()
          .stream()
          .map(e -> Map.entry(e.getKey().toString(), e.getValue().toString()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
      LOGGER.info("Restoring constraints and indexes for target database on tombstone signal");
      try (
          SessionFactory sessionFactory = config.getHibernateConfiguration().buildSessionFactory();
          StatelessSession session = sessionFactory.openStatelessSession();
          ) {
        Transaction transaction = session.beginTransaction();
        try {
          session.createNativeQuery(
              SnapshotHelperQueries.RESTORE_INDEXES.readSql(),
              Object.class).executeUpdate();
          session.createNativeQuery(
              SnapshotHelperQueries.RESTORE_CONSTRAINTS.readSql(),
              Object.class).executeUpdate();
          transaction.commit();
        } catch (RuntimeException ex) {
          transaction.rollback();
          throw ex;
        } catch (Exception ex) {
          transaction.rollback();
          throw new RuntimeException(ex);
        }
      }
    }
  }

  @SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE",
      justification = "False positive")
  private void cleanupSource() {
    if (Objects.equals(stream.getSpec().getSource().getType(), StreamSourceType.SGCLUSTER.toString())
        || Objects.equals(stream.getSpec().getSource().getType(), StreamSourceType.POSTGRES.toString())) {
      final String slotName = SgClusterDebeziumEngineHandler.slotName(stream);
      final String publicationName = SgClusterDebeziumEngineHandler.publicationName(stream);
      final Properties props;
      if (Objects.equals(stream.getSpec().getSource().getType(), StreamSourceType.SGCLUSTER.toString())) {
        props = getDebeziumJdbcSinkConnectorConfigSgCluster();
      } else {
        props = getDebeziumJdbcSinkConnectorConfigPostgres();
      }
      final JdbcSinkConnectorConfig config = new JdbcSinkConnectorConfig(props
          .entrySet()
          .stream()
          .map(e -> Map.entry(e.getKey().toString(), e.getValue().toString()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
      LOGGER.info("Dropping replication slot and publication, if exists, on tombstone signal");
      try (
          SessionFactory sessionFactory = config.getHibernateConfiguration().buildSessionFactory();
          StatelessSession session = sessionFactory.openStatelessSession();
          ) {
        Transaction transaction = session.beginTransaction();
        try {
          session.createNativeQuery(
              """
              DO $$BEGIN
                PERFORM pg_drop_replication_slot(slot_name)
                FROM pg_replication_slots
                WHERE slot_name = '%1$s';
              END$$
              """.formatted(slotName), Object.class).executeUpdate();
          session.createNativeQuery(
              """
              DROP PUBLICATION IF EXISTS "%1$s"
              """.formatted(publicationName), Object.class).executeUpdate();
          transaction.commit();
        } catch (RuntimeException ex) {
          transaction.rollback();
          throw ex;
        } catch (Exception ex) {
          transaction.rollback();
          throw new RuntimeException(ex);
        }
      }
    }
  }

  private Properties getDebeziumJdbcSinkConnectorConfigSgCluster() {
    final Properties props = new Properties();
    final var sgCluster = Optional.of(stream.getSpec().getSource().getSgCluster());
    final String namespace = stream.getMetadata().getNamespace();
    final String clusterName = sgCluster.map(StackGresStreamSourceSgCluster::getName)
        .orElseThrow(() -> new IllegalArgumentException("The name of SGCluster is not specified"));
    final StackGresCluster cluster = clusterFinder.findByNameAndNamespace(clusterName, namespace)
        .orElseThrow(() -> new IllegalArgumentException(StackGresCluster.KIND + " " + clusterName + " not found"));
    final String clusterServiceName = PatroniUtil.readWriteName(cluster);
    final String clusterPort = String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT);
    final String clusterDatabase = Optional.ofNullable(stream.getSpec().getSource().getSgCluster())
        .map(StackGresStreamSourceSgCluster::getDatabase)
        .orElse("postgres");
    final String usernameSecretName = sgCluster
        .map(StackGresStreamSourceSgCluster::getUsername)
        .map(SecretKeySelector::getName)
        .orElseGet(() -> PatroniUtil.secretName(clusterName));
    final String usernameSecretKey = sgCluster
        .map(StackGresStreamSourceSgCluster::getUsername)
        .map(SecretKeySelector::getKey)
        .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_USERNAME_KEY);
    final var username = getSecretKeyValue(namespace, usernameSecretName, usernameSecretKey);
    final String passwordSecretName = sgCluster
        .map(StackGresStreamSourceSgCluster::getPassword)
        .map(SecretKeySelector::getName)
        .orElseGet(() -> PatroniUtil.secretName(clusterName));
    final String passwordSecretKey = sgCluster
        .map(StackGresStreamSourceSgCluster::getPassword)
        .map(SecretKeySelector::getKey)
        .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY);
    final var password = getSecretKeyValue(namespace, passwordSecretName, passwordSecretKey);
 
    props.setProperty("connection.username", username);
    props.setProperty("connection.password", password);
    props.setProperty("connection.url", "jdbc:postgresql://%s:%s/%s"
        .formatted(
            clusterServiceName,
            clusterPort,
            clusterDatabase));
    return props;
  }

  private Properties getDebeziumJdbcSinkConnectorConfigPostgres() {
    final Properties props = new Properties();
    final var postgres = Optional.of(stream.getSpec().getSource().getPostgres());
    final String namespace = stream.getMetadata().getNamespace();
    final String clusterHost = postgres.map(StackGresStreamSourcePostgres::getHost)
        .orElseThrow(() -> new IllegalArgumentException("The postgres host is not specified"));
    final String clusterPort = postgres.map(StackGresStreamSourcePostgres::getPort)
        .map(Object::toString)
        .orElse("5432");
    final String clusterDatabase = Optional.ofNullable(stream.getSpec().getSource().getPostgres())
        .map(StackGresStreamSourcePostgres::getDatabase)
        .orElse("postgres");
    final String usernameSecretName = postgres
        .map(StackGresStreamSourcePostgres::getUsername)
        .map(SecretKeySelector::getName)
        .orElseThrow(() -> new IllegalArgumentException("The postgres username secret is not specified"));
    final String usernameSecretKey = postgres
        .map(StackGresStreamSourcePostgres::getUsername)
        .map(SecretKeySelector::getKey)
        .orElseThrow(() -> new IllegalArgumentException("The postgres username secret key is not specified"));
    final var username = getSecretKeyValue(namespace, usernameSecretName, usernameSecretKey);
    final String passwordSecretName = postgres
        .map(StackGresStreamSourcePostgres::getPassword)
        .map(SecretKeySelector::getName)
        .orElseThrow(() -> new IllegalArgumentException("The postgres password secret is not specified"));
    final String passwordSecretKey = postgres
        .map(StackGresStreamSourcePostgres::getPassword)
        .map(SecretKeySelector::getKey)
        .orElseThrow(() -> new IllegalArgumentException("The postgres password secret key is not specified"));
    final var password = getSecretKeyValue(namespace, passwordSecretName, passwordSecretKey);
 
    props.setProperty("connection.username", username);
    props.setProperty("connection.password", password);
    props.setProperty("connection.url", "jdbc:postgresql://%s:%s/%s"
        .formatted(
            clusterHost,
            clusterPort,
            clusterDatabase));
    return props;
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
