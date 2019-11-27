/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.resource.AbstractResourceHandler;

@ApplicationScoped
public class PrometheusEndpointsHandler extends AbstractResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource instanceof Endpoints
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
