/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.pipeline.signal.SignalRecord;
import io.debezium.pipeline.signal.channels.SignalChannelReader;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StreamPath;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.stream.app.StreamProperty;

public class DebeziumAnnotationSignalChannelReader implements SignalChannelReader {

  private static final String STACKGRES_IO_DEBEZIUM_SIGNAL_KEY_PREFIX =
      "debezium-signal." + StackGresContext.STACKGRES_KEY_PREFIX;

  static CustomResourceFinder<StackGresStream> streamFinder;

  public static void setStreamFinder(CustomResourceFinder<StackGresStream> streamFinder) {
    DebeziumAnnotationSignalChannelReader.streamFinder = streamFinder;
  }

  String streamName;
  String streamNamespace;
  Path annotationSignalPropertiesPath;

  @Override
  public String name() {
    return "sgstream-annotations";
  }

  @Override
  public void init(CommonConnectorConfig connectorConfig) {
    this.streamName = StreamProperty.STREAM_NAME.getString();
    this.streamNamespace = StreamProperty.STREAM_NAMESPACE.getString();
    this.annotationSignalPropertiesPath = Paths.get(StreamPath.DEBEZIUM_DATABASE_HISTORY_PATH.path());
  }

  @Override
  public List<SignalRecord> read() {
    Properties annotationSignalProperties = new Properties();
    if (Files.exists(annotationSignalPropertiesPath)) {
      try (InputStream inputStream = Files.newInputStream(annotationSignalPropertiesPath)) {
        annotationSignalProperties.load(inputStream);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
    final var streamSignalAnnotations =
        streamFinder.findByNameAndNamespace(streamName, streamNamespace)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .filter(annotation -> annotation.getKey().startsWith(STACKGRES_IO_DEBEZIUM_SIGNAL_KEY_PREFIX))
        .filter(annotation -> Optional
            .ofNullable(annotationSignalProperties.getProperty(annotation.getKey()))
            .map(value -> !annotation.getValue().equals(value))
            .orElse(true));
    streamSignalAnnotations.forEach(annotation -> annotationSignalProperties
        .setProperty(annotation.getKey(), annotation.getValue()));
    if (!Files.exists(annotationSignalPropertiesPath)) {
      try {
        Files.createFile(annotationSignalPropertiesPath);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
    try (OutputStream outputStream = Files.newOutputStream(annotationSignalPropertiesPath)) {
      annotationSignalProperties.store(outputStream, null);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return streamSignalAnnotations
        .map(annotation -> new SignalRecord(
            annotation.getKey(),
            extractType(annotation.getKey()),
            annotation.getValue(),
            Map.of()))
        .toList();
  }

  private String extractType(String key) {
    int indexOfFirstSlash = key.indexOf("/");
    if (indexOfFirstSlash < 0) {
      return null;
    }
    return key.substring(indexOfFirstSlash + 1);
  }

  @Override
  public void close() {
  }

}
