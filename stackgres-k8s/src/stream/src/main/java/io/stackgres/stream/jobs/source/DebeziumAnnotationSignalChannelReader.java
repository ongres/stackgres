/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.source;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.pipeline.signal.SignalRecord;
import io.debezium.pipeline.signal.channels.SignalChannelReader;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.stream.app.StreamProperty;

public class DebeziumAnnotationSignalChannelReader implements SignalChannelReader {

  private static final String STACKGRES_IO_DEBEZIUM_SIGNAL_KEY_PREFIX = "debezium-signal." + StackGresContext.STACKGRES_KEY_PREFIX;

  static CustomResourceFinder<StackGresStream> streamFinder;

  public static void setStreamFinder(CustomResourceFinder<StackGresStream> streamFinder) {
    DebeziumAnnotationSignalChannelReader.streamFinder = streamFinder;
  }

  String streamName;
  String streamNamespace;

  @Override
  public String name() {
    return "sgstream-annotations";
  }

  @Override
  public void init(CommonConnectorConfig connectorConfig) {
    this.streamName = StreamProperty.STREAM_NAME.getString();
    this.streamNamespace = StreamProperty.STREAM_NAMESPACE.getString();
  }

  @Override
  public List<SignalRecord> read() {
    return streamFinder.findByNameAndNamespace(streamName, streamNamespace)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .filter(annotation -> annotation.getKey().startsWith(STACKGRES_IO_DEBEZIUM_SIGNAL_KEY_PREFIX))
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
