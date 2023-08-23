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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;

@Path(MutationUtil.BACKUPCONFIG_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackupConfigMutationResource
    extends AbstractMutationResource<StackGresBackupConfig, BackupConfigReview> {

  @Inject
  public BackupConfigMutationResource(
      ObjectMapper objectMapper,
      MutationPipeline<StackGresBackupConfig, BackupConfigReview> pipeline) {
    super(objectMapper, pipeline);
  }

  public BackupConfigMutationResource() {
    super(null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    getLogger().info("Backup configuration mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(BackupConfigReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresBackupConfig> getResourceClass() {
    return StackGresBackupConfig.class;
  }
}
