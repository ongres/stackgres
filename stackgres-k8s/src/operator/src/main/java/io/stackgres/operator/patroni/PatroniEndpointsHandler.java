/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.resource.AbstractClusterResourceHandler;

@ApplicationScoped
public class PatroniEndpointsHandler extends AbstractClusterResourceHandler {

  @Override
  public boolean isHandlerForResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource instanceof Endpoints
        && resource.getMetadata().getNamespace().equals(
            config.getCluster().getMetadata().getNamespace())
        && (resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName())
        || resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName() + PatroniServices.READ_WRITE_SERVICE)
        || resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName() + PatroniServices.READ_ONLY_SERVICE)
        || resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName() + PatroniServices.FAILOVER_SERVICE));
  }

  @Override
  public boolean isManaged() {
    return true;
  }

}
