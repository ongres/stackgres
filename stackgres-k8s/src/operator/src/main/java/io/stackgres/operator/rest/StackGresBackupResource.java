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
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;

@Path("/stackgres/backup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresBackupResource
    extends AbstractCustomResourceRestService<StackGresBackup> {

  @Inject
  public StackGresBackupResource(KubernetesCustomResourceScanner<StackGresBackup> scanner,
      KubernetesCustomResourceFinder<StackGresBackup> finder,
      CustomResourceScheduler<StackGresBackup> scheduler) {
    super(scanner, finder, scheduler);
  }

  public StackGresBackupResource() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
