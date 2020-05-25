/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.apiweb.distributedlogs.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.apiweb.transformer.DependencyResourceTransformer;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;

@Path("/stackgres/sgdistributedlogs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DistributedLogsResource
    extends AbstractDependencyRestService<DistributedLogsDto, StackGresDistributedLogs> {

  @Inject
  public DistributedLogsResource(
      CustomResourceScanner<StackGresDistributedLogs> scanner,
      CustomResourceFinder<StackGresDistributedLogs> finder,
      CustomResourceScheduler<StackGresDistributedLogs> scheduler,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      DependencyResourceTransformer<DistributedLogsDto, StackGresDistributedLogs> transformer) {
    super(scanner, finder, scheduler, clusterScanner, transformer);
  }

  public DistributedLogsResource() {
    super(null, null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  public boolean belongsToCluster(StackGresDistributedLogs resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(Optional.ofNullable(cluster.getSpec().getDistributedLogs())
            .map(StackGresClusterDistributedLogs::getDistributedLogs),
            Optional.of(resource.getMetadata().getName()));
  }

}
