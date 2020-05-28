/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.apiweb.distributedlogs.dto.backup.BackupDto;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;

@Path("/stackgres/sgbackup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackupResource
    extends AbstractRestService<BackupDto, StackGresBackup> {

  @Inject
  public BackupResource(CustomResourceScanner<StackGresBackup> scanner,
      CustomResourceFinder<StackGresBackup> finder,
      CustomResourceScheduler<StackGresBackup> scheduler,
      ResourceTransformer<BackupDto, StackGresBackup> transformer) {
    super(scanner, finder, scheduler, transformer);
  }

  public BackupResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
