/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.configuration;

import javax.management.MalformedObjectNameException;

import io.stackgres.common.metrics.AbstractJmxCollectorRegistry;
import io.stackgres.stream.app.StreamProperty;
import jakarta.inject.Singleton;

@Singleton
public class StreamJmxCollectorRegistry extends AbstractJmxCollectorRegistry {

  public StreamJmxCollectorRegistry() throws MalformedObjectNameException {
    super(StreamProperty.STREAM_JMX_COLLECTOR_YAML_CONFIG.get().orElse(""));
  }

}
