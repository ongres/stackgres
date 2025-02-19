/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.configuration;

import javax.management.MalformedObjectNameException;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.metrics.AbstractJmxCollectorRegistry;
import jakarta.inject.Singleton;

@Singleton
public class OperatorJmxCollectorRegistry extends AbstractJmxCollectorRegistry {

  public OperatorJmxCollectorRegistry() throws MalformedObjectNameException {
    super(OperatorProperty.JMX_COLLECTOR_YAML_CONFIG.get().orElse(""));
  }

}
