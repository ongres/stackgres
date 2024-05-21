/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.DebeziumEngine.CompletionCallback;
import io.debezium.engine.format.SerializationFormat;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StreamPath;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgstream.DebeziumDefault;
import io.stackgres.common.crd.sgstream.DebeziumListSeparator;
import io.stackgres.common.crd.sgstream.DebeziumMapSeparator;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamDebeziumEngineProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamSourcePostgresConnectorProperties;
import io.stackgres.common.crd.sgstream.StackGresStreamSourceSgCluster;
import io.stackgres.common.crd.sgstream.StackGresStreamSpec;
import io.stackgres.common.crd.sgstream.StreamSourceType;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DebeziumEngineHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DebeziumEngineHandler.class);

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

  public <T> CompletableFuture<Void> streamChangeEvents(
      StackGresStream stream,
      Class<? extends SerializationFormat<T>> format,
      Consumer<ChangeEvent<T, T>> eventConsumer) {
    final Properties props = new Properties();
    /* engine properties */
    String namespace = stream.getMetadata().getNamespace();
    props.setProperty("name", name(stream));
    props.setProperty("topic.prefix", name(stream));
    props.setProperty("offset.storage",
        "org.apache.kafka.connect.storage.FileOffsetBackingStore");
    props.setProperty("offset.storage.file.filename", StreamPath.DEBEZIUM_OFFSET_STORAGE_PATH.path());
    props.setProperty("database.history",
        "io.debezium.relational.history.FileDatabaseHistory");
    props.setProperty("database.history.file.filename", StreamPath.DEBEZIUM_DATABASE_HISTORY_PATH.path());
    configureDebeziumSectionProperties(
        props,
        Optional.of(stream.getSpec())
        .map(StackGresStreamSpec::getDebeziumEngineProperties)
        .orElse(null),
        StackGresStreamDebeziumEngineProperties.class);
    if (Objects.equals(stream.getSpec().getSource().getType(), StreamSourceType.SGCLUSTER.toString())) {
      var sgCluster = Optional.of(stream.getSpec().getSource().getSgCluster());
      configureDebeziumSectionProperties(
          props,
          sgCluster
          .map(StackGresStreamSourceSgCluster::getDebeziumConnectorProperties)
          .orElse(null),
          StackGresStreamSourcePostgresConnectorProperties.class);
      
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

      /* begin connector properties */
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

    CompletableFuture<Void> completed = new CompletableFuture<>();
    final DebeziumEngine<?> engine;
    try {
      engine = DebeziumEngine.create(format)
          .using(props)
          .using(new CompletionCallback() {
            @Override
            public void handle(boolean success, String message, Throwable error) {
              if (success) {
                LOGGER.info("Debezium Engine process completed");
                completed.complete(null);
              } else {
                if (error != null) {
                  LOGGER.error("Debezium Engine process failed", error);
                  completed.completeExceptionally(error);
                } else {
                  LOGGER.error("Debezium Engine process failed: {}", message);
                  completed.completeExceptionally(new RuntimeException(message));
                }
              }
            }
          })
          .notifying(eventConsumer)
          .build();
    } catch (Exception ex) {
      completed.completeExceptionally(new RuntimeException("Debezium Engine initialization failed", ex));
      return completed;
    }
    try {
      executor.execute(engine);
      return completed.handleAsync(Unchecked.biFunction((ignored, ex) -> {
        try {
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
      completed.completeExceptionally(new RuntimeException("Debezium Engine initialization failed", ex));
      return completed;
    }
  }

  private void configureDebeziumSectionProperties(
      Properties props,
      Object debeziumSectionProperties,
      Class<?> debeziumSectionPropertiesClass) {
    for (Field field : debeziumSectionPropertiesClass.getDeclaredFields()) {
      if (!List.of(String.class, Boolean.class, Integer.class, List.class).contains(field.getType())) {
        continue;
      }
      String property = field.getName().replaceAll("([A-Z])", ".$1").toLowerCase();
      String getterMethodName = "get" + field.getName().substring(0, 1).toUpperCase()
          + field.getName().substring(1);
      Method getterMethod;
      try {
        getterMethod = debeziumSectionPropertiesClass.getMethod(getterMethodName);
        Object value = debeziumSectionProperties != null ? getterMethod.invoke(debeziumSectionProperties) : null;
        value = Optional.ofNullable(field.getAnnotation(DebeziumDefault.class))
            .<Object>map(DebeziumDefault::value)
            .orElse(value);
        if (value != null) {
          if (value instanceof Map map) {
            setMapProperties(props, field, property, map);
          } else if (value instanceof List list) {
            props.setProperty(property, joinList(field, list));
          } else {
            props.setProperty(property, value.toString());
          }
        }
      } catch (RuntimeException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void setMapProperties(Properties props, Field field, String property, Map<?, ?> map) {
    Map<String, ?> mapWithKeys = (Map<String, ?>) map;
    var mapSeparator = Optional.ofNullable(field.getAnnotation(DebeziumMapSeparator.class));
    for (var entry : mapWithKeys.entrySet()) {
      if (getMapSeparatorValueFromLevel(mapSeparator) <= 0) {
        props.setProperty(property,
            entry.getKey()
                + getMapSeparatorAtLevel0(mapSeparator)
                + entry.getValue().toString());
        continue;
      }
      String entryProperty = property + getMapSeparatorAtLevel0(mapSeparator) + entry.getKey();
      if (entry.getValue() instanceof Map entryValueMap) {
        Map<String, ?> entryValueMapWithKeys = (Map<String, ?>) entryValueMap;
        for (var entryValueEntry : entryValueMapWithKeys.entrySet()) {
          String entryValueEntryProperty =
              entryProperty + getMapSeparatorAtLevel1(mapSeparator) + entryValueEntry.getKey();
          if (entry.getValue() instanceof List entryValueEntryValueList) {
            props.setProperty(entryValueEntryProperty, joinList(field, entryValueEntryValueList));
          } else {
            props.setProperty(entryValueEntryProperty, entry.getValue().toString());
          }
        }
      } else {
        props.setProperty(entryProperty, entry.getValue().toString());
      }
    }
  }

  private int getMapSeparatorValueFromLevel(Optional<DebeziumMapSeparator> mapSeparator) {
    return mapSeparator.map(DebeziumMapSeparator::valueFromLevel)
        .orElse(DebeziumMapSeparator.DEFAULT_VALUE_FROM_LEVEL);
  }

  private String getMapSeparatorAtLevel0(Optional<DebeziumMapSeparator> mapSeparator) {
    return mapSeparator.map(DebeziumMapSeparator::separatorLevel0)
        .orElse(DebeziumMapSeparator.DEFAULT_MAP_SEPARATOR);
  }

  private String getMapSeparatorAtLevel1(Optional<DebeziumMapSeparator> mapSeparator) {
    return mapSeparator.map(DebeziumMapSeparator::separatorLevel1)
        .orElse(DebeziumMapSeparator.DEFAULT_MAP_SEPARATOR);
  }

  private String joinList(Field field, List<?> list) {
    return list.stream()
        .map(Object::toString)
        .collect(Collectors.joining(
            Optional.ofNullable(field.getAnnotation(DebeziumListSeparator.class))
            .map(DebeziumListSeparator::value)
            .orElse(",")));
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
