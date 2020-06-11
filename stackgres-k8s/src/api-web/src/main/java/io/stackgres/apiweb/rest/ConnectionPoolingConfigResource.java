/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.transformer.DependencyResourceTransformer;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;

@Path("/stackgres/sgpoolconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectionPoolingConfigResource extends
    AbstractDependencyRestService<PoolingConfigDto, StackGresPoolingConfig> {

  @Inject
  public ConnectionPoolingConfigResource(
      CustomResourceScanner<StackGresPoolingConfig> scanner,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      DependencyResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> transformer) {
    super(scanner, finder, scheduler, clusterScanner, transformer);
  }

  public ConnectionPoolingConfigResource() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  public boolean belongsToCluster(StackGresPoolingConfig resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getConfiguration().getConnectionPoolingConfig(),
            resource.getMetadata().getName());
  }

}
