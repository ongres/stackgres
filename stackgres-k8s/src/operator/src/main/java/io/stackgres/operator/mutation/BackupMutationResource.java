/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.common.BackupReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;

@Path(MutationUtil.BACKUP_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackupMutationResource extends MutationResource<StackGresBackup, BackupReview> {

  @Inject
  public BackupMutationResource(
      MutationPipeline<StackGresBackup, BackupReview> pipeline) {
    super(pipeline);
  }

  public BackupMutationResource() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    getLogger().info("Backup mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(BackupReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresBackup> getResourceClass() {
    return StackGresBackup.class;
  }
}
