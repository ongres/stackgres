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
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigDto;
import io.stackgres.operator.rest.transformer.ResourceTransformer;

@Path("/stackgres/backupconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackupConfigResource extends
    AbstractRestService<BackupConfigDto, StackGresBackupConfig> {

  @Inject
  public BackupConfigResource(
      CustomResourceScanner<StackGresBackupConfig> scanner,
      CustomResourceFinder<StackGresBackupConfig> finder,
      CustomResourceScheduler<StackGresBackupConfig> scheduler,
      ResourceTransformer<BackupConfigDto, StackGresBackupConfig> transformer) {
    super(scanner, finder, scheduler, transformer);
  }

  public BackupConfigResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

}
