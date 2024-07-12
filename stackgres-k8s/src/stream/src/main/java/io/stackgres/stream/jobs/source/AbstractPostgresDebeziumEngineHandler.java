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
import io.stackgres.common.StreamPath;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamDebeziumEngineProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgres;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgresDebeziumProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import io.stackgres.common.crd.sgstream.StackGresStreamSpec;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.stream.jobs.DebeziumUtil;
import io.stackgres.stream.jobs.SourceEventHandler;
import io.stackgres.stream.jobs.StreamDebeziumSignalActionProvider;
import io.stackgres.stream.jobs.StreamExecutorService;
import io.stackgres.stream.jobs.TargetEventConsumer;
import io.stackgres.stream.jobs.target.migration.StreamMigrationTableNamingStrategy;
import jakarta.inject.Inject;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPostgresDebeziumEngineHandler implements SourceEventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPostgresDebeziumEngineHandler.class);

  @Inject
  ResourceFinder<Secret> secretFinder;

  @Inject
  CustomResourceFinder<StackGresStream> streamFinder;

  @Inject
  CustomResourceScheduler<StackGresStream> streamScheduler;

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

  public static String slotName(StackGresStream stream) {
    return Optional.ofNullable(stream.getSpec().getSource().getSgCluster())
        .filter(sgCluster -> Objects.equals(
            stream.getSpec().getSource().getType(),
            StreamSourceType.SGCLUSTER.toString()))
        .map(StackGresStreamSourceSgCluster::getDebeziumProperties)
        .or(() -> Optional.ofNullable(stream.getSpec().getSource().getPostgres())
            .filter(postgres -> Objects.equals(
                stream.getSpec().getSource().getType(),
                StreamSourceType.POSTGRES.toString()))
            .map(StackGresStreamSourcePostgres::getDebeziumProperties))
        .map(StackGresStreamSourcePostgresDebeziumProperties::getSlotName)
        .orElseGet(() -> (stream.getMetadata().getNamespace() + "_" + stream.getMetadata().getName())
            .replaceAll("[^a-zA-Z0-9_]", "_"));
  }

  public static String publicationName(StackGresStream stream) {
    return slotName(stream);
  }

  @Override
  public <T> CompletableFuture<Void> streamChangeEvents(
      StackGresStream stream,
      Class<? extends SerializationFormat<T>> format,
      TargetEventConsumer<T> eventConsumer) {
    StreamMigrationTableNamingStrategy.setTopicPrefix(name(stream));
    DebeziumAnnotationSignalChannelReader.setStreamFinder(streamFinder);

    final Properties props = new Properties();
    props.setProperty("name", name(stream));
    props.setProperty("topic.prefix", name(stream));
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
    setSourceProperties(stream, props);

    final CompletableFuture<Void> streamCompleted = new CompletableFuture<>();
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
      throw new RuntimeException("Debezium Engine initialization failed", ex);
    }
    try {
      TombstoneDebeziumSignalAction tombstoneSignalAction = new TombstoneDebeziumSignalAction(
          stream, streamScheduler, secretFinder, engine, streamCompleted);
      StreamDebeziumSignalActionProvider.registerSignalAction(
          StreamDebeziumSignalActionProvider.TOMBSTONE_SIGNAL_TYPE, tombstoneSignalAction);
      executor.execute(engine);
      return streamCompleted
          .handleAsync(Unchecked.biFunction((ignored, ex) -> {
            try {
              LOGGER.info("Shutting down Event Handler");
              eventConsumer.close();
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
          }))
          .thenComposeAsync(ignored -> tombstoneSignalAction.getTombstoneCompleted());
    } catch (Exception ex) {
      try {
        engine.close();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
      } catch (Exception engineEx) {
        ex.addSuppressed(engineEx);
      }
      throw new RuntimeException("Debezium Engine initialization failed", ex);
    }
  }

  protected abstract void setSourceProperties(StackGresStream stream, final Properties props);

  protected String getSecretKeyValue(String namespace, String secretName, String secretKey) {
    return Optional.of(secretFinder.findByNameAndNamespace(secretName, namespace)
        .orElseThrow(() -> new IllegalArgumentException("Secret " + secretName + " not found")))
        .map(Secret::getData)
        .map(data -> data.get(secretKey))
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new IllegalArgumentException("key " + secretKey + " not found in Secret " + secretName));
  }

}
