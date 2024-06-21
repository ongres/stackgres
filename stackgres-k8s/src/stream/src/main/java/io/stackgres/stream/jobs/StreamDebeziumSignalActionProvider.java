/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.pipeline.ChangeEventSourceCoordinator;
import io.debezium.pipeline.EventDispatcher;
import io.debezium.pipeline.signal.actions.SignalAction;
import io.debezium.pipeline.signal.actions.SignalActionProvider;
import io.debezium.pipeline.spi.Partition;
import io.debezium.spi.schema.DataCollectionId;

public class StreamDebeziumSignalActionProvider implements SignalActionProvider {

  public static final String TOMBSTONE_SIGNAL_TYPE = "tombstone";
  public static final String COMMAND_SIGNAL_TYPE = "command";

  private static Map<String, SignalAction<?>> signalActions = new HashMap<>();

  public static void registerSignalAction(String name, SignalAction<?> signalAction) {
    signalActions.put(name, signalAction);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <P extends Partition> Map<String, SignalAction<P>> createActions(
      EventDispatcher<P, ? extends DataCollectionId> dispatcher,
      ChangeEventSourceCoordinator<P, ?> changeEventSourceCoordinator, CommonConnectorConfig connectorConfig) {
    return signalActions.entrySet()
        .stream()
        .map(e -> Map.entry(e.getKey(), (SignalAction<P>) e.getValue()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

}
