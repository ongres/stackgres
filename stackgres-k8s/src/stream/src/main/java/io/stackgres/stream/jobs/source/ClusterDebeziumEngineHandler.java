/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.source;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.CompletionCallback;
import io.debezium.engine.format.SerializationFormat;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StreamPath;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamDebeziumEngineProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgresDebeziumProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import io.stackgres.common.crd.sgstream.StackGresStreamSpec;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.stream.jobs.DebeziumUtil;
import io.stackgres.stream.jobs.SourceEventHandler;
import io.stackgres.stream.jobs.StreamExecutorService;
import io.stackgres.stream.jobs.StreamSourceOperation;
import io.stackgres.stream.jobs.TargetEventConsumer;
import io.stackgres.stream.jobs.target.migration.StreamMigrationTableNamingStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@StreamSourceOperation(StreamSourceType.SGCLUSTER)
public class ClusterDebeziumEngineHandler implements SourceEventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDebeziumEngineHandler.class);

  @Inject
  ResourceFinder<Secret> secretFinder;

  @Inject
  StreamExecutorService executorService;

  final ExecutorService executor = Executors.newSingleThreadExecutor(
      r -> new Thread(r, "DebeziumEngine"));

  public static String name(StackGresStream stream) {
    return stream.getMetadata().getNamespace() + "." + stream.getMetadata().getName();
  }

  public static String topicPrefix(StackGresStream stream) {
    return name(stream);
  }

  @Override
  public <T> CompletableFuture<Void> streamChangeEvents(
      StackGresStream stream,
      Class<? extends SerializationFormat<T>> format,
      TargetEventConsumer<T> eventConsumer,
      Runnable closer) {
    final Properties props = new Properties();
    String namespace = stream.getMetadata().getNamespace();
    props.setProperty("name", name(stream));
    props.setProperty("topic.prefix", name(stream));
    StreamMigrationTableNamingStrategy.setTopicPrefix(name(stream));
    props.setProperty("offset.storage",
        "org.apache.kafka.connect.storage.FileOffsetBackingStore");
    props.setProperty("offset.storage.file.filename", StreamPath.DEBEZIUM_OFFSET_STORAGE_PATH.path());
    props.setProperty("database.history",
        "io.debezium.relational.history.FileDatabaseHistory");
    props.setProperty("database.history.file.filename", StreamPath.DEBEZIUM_DATABASE_HISTORY_PATH.path());
    DebeziumUtil.configureDebeziumSectionProperties(
        props,
        Optional.of(stream.getSpec())
        .map(StackGresStreamSpec::getDebeziumEngineProperties)
        .orElse(null),
        StackGresStreamDebeziumEngineProperties.class);
    if (Objects.equals(stream.getSpec().getSource().getType(), StreamSourceType.SGCLUSTER.toString())) {
      var sgCluster = Optional.of(stream.getSpec().getSource().getSgCluster());
      DebeziumUtil.configureDebeziumSectionProperties(
          props,
          sgCluster
          .map(StackGresStreamSourceSgCluster::getDebeziumProperties)
          .orElse(null),
          StackGresStreamSourcePostgresDebeziumProperties.class);
      
      String clusterName = sgCluster.map(StackGresStreamSourceSgCluster::getName)
          .orElseThrow(() -> new IllegalArgumentException("The name of SGCluster is not specified"));
      String usernameSecretName = sgCluster
          .map(StackGresStreamSourceSgCluster::getUsername)
          .map(SecretKeySelector::getName)
          .orElseGet(() -> PatroniUtil.secretName(clusterName));
      String usernameSecretKey = sgCluster
          .map(StackGresStreamSourceSgCluster::getUsername)
          .map(SecretKeySelector::getKey)
          .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_USERNAME_KEY);
      var username = getSecretKeyValue(namespace, usernameSecretName, usernameSecretKey);
      String passwordSecretName = sgCluster
          .map(StackGresStreamSourceSgCluster::getPassword)
          .map(SecretKeySelector::getName)
          .orElseGet(() -> PatroniUtil.secretName(clusterName));
      String passwordSecretKey = sgCluster
          .map(StackGresStreamSourceSgCluster::getPassword)
          .map(SecretKeySelector::getKey)
          .orElseGet(() -> StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY);
      var password = getSecretKeyValue(namespace, passwordSecretName, passwordSecretKey);

      props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
      props.setProperty("database.hostname", clusterName);
      props.setProperty("database.port", String.valueOf(PatroniUtil.REPLICATION_SERVICE_PORT));
      props.setProperty("database.dbname", Optional
          .ofNullable(stream.getSpec().getSource().getSgCluster())
          .map(StackGresStreamSourceSgCluster::getDatabase)
          .orElse("postgres"));
      props.setProperty("database.user", username);
      props.setProperty("database.password", password);
      props.setProperty("database.server.name", clusterName);
    }

    CompletableFuture<Void> streamCompleted = new CompletableFuture<>();
    final DebeziumEngine<?> engine;
    try {
      engine = DebeziumEngine.create(format)
          .using(props)
          .using(new CompletionCallback() {
            @Override
            public void handle(boolean success, String message, Throwable error) {
              if (success) {
                LOGGER.info("Debezium Engine process completed");
                streamCompleted.complete(null);
              } else {
                if (error != null) {
                  LOGGER.error("Debezium Engine process failed", error);
                  streamCompleted.completeExceptionally(error);
                } else {
                  LOGGER.error("Debezium Engine process failed: {}", message);
                  streamCompleted.completeExceptionally(new RuntimeException(message));
                }
              }
            }
          })
          .notifying(eventConsumer::consumeEvent)
          .build();
    } catch (Exception ex) {
      streamCompleted.completeExceptionally(new RuntimeException("Debezium Engine initialization failed", ex));
      return streamCompleted;
    }
    try {
      executor.execute(engine);
      return streamCompleted.handleAsync(Unchecked.biFunction((ignored, ex) -> {
        try {
          LOGGER.info("Shutting down Event Handler");
          closer.run();
          LOGGER.info("Shutting down Debezium Engine");
          engine.close();
          executor.shutdown();
          executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (Exception shutdownEx) {
          if (ex == null) {
            ex = shutdownEx;
          } else {
            ex.addSuppressed(shutdownEx);
          }
        }
        if (ex != null) {
          throw ex;
        }
        return null;
      }));
    } catch (Exception ex) {
      try {
        engine.close();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
      } catch (Exception engineEx) {
        ex.addSuppressed(engineEx);
      }
      streamCompleted.completeExceptionally(new RuntimeException("Debezium Engine initialization failed", ex));
      return streamCompleted;
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
