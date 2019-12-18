/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operator.resource.KubernetesResourceScanner;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;

@Path("/stackgres/connpoolconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresConnectionPoolingConfigResource extends
    AbstractCustomResourceRestService<StackGresPgbouncerConfig, StackGresPgbouncerConfigList> {

  @Inject
  public StackGresConnectionPoolingConfigResource(
      KubernetesResourceScanner<StackGresPgbouncerConfigList> scanner,
      KubernetesCustomResourceFinder<StackGresPgbouncerConfig> finder,
      CustomResourceScheduler<StackGresPgbouncerConfig> scheduler) {
    super(scanner, finder, scheduler, StackGresPgbouncerConfigDefinition.NAME);
  }

  public StackGresConnectionPoolingConfigResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
