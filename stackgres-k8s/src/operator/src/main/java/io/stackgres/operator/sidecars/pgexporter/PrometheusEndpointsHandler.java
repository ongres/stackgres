/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.resource.AbstractResourceHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PrometheusEndpointsHandler extends AbstractResourceHandler {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PrometheusEndpointsHandler.class);

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Prometheus monitored endpoints handler registered");
  }

  @Override
  public boolean handleResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource.getKind().equals("Endpoints")
        && resource.getMetadata().getNamespace().equals(
            config.getCluster().getMetadata().getNamespace())
        && resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName() + PostgresExporter.EXPORTER_SERVICE);
  }

  @Override
  public boolean isManaged() {
    return true;
  }

}
