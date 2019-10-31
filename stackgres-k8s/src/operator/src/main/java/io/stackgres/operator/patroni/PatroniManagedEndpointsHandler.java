/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.resource.AbstractResourceHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PatroniManagedEndpointsHandler extends AbstractResourceHandler {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PatroniManagedEndpointsHandler.class);

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Patroni managed endpoints handler registered");
  }

  @Override
  public boolean handleResource(StackGresClusterConfig config, HasMetadata resource) {
    return config != null
        && resource.getKind().equals("Endpoints")
        && resource.getMetadata().getNamespace().equals(
            config.getCluster().getMetadata().getNamespace())
        && (resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName())
        || resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName() + PatroniServices.READ_WRITE_SERVICE)
        || resource.getMetadata().getName().equals(
            config.getCluster().getMetadata().getName() + PatroniServices.READ_ONLY_SERVICE));
  }

  @Override
  public boolean isManaged() {
    return true;
  }

}
