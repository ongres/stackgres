/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.configuration;

import javax.management.MalformedObjectNameException;

import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.metrics.AbstractJmxCollectorRegistry;
import jakarta.inject.Singleton;

@Singleton
public class ClusterControllerJmxCollectorRegistry extends AbstractJmxCollectorRegistry {

  public ClusterControllerJmxCollectorRegistry() throws MalformedObjectNameException {
    super(ClusterControllerProperty.CLUSTER_CONTROLLER_JMX_COLLECTOR_YAML_CONFIG
        .get().orElse(""));
  }

}
