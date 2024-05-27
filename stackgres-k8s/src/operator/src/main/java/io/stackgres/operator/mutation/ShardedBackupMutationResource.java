/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.common.ShardedBackupReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(MutationUtil.SHARDED_BACKUP_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShardedBackupMutationResource
    extends AbstractMutationResource<StackGresShardedBackup, ShardedBackupReview> {

  @Inject
  public ShardedBackupMutationResource(
      ObjectMapper objectMapper,
      MutationPipeline<StackGresShardedBackup, ShardedBackupReview> pipeline) {
    super(OperatorProperty.getAllowedNamespaces(), objectMapper, pipeline);
  }

  public ShardedBackupMutationResource() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    getLogger().info("Sharded Backup mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(ShardedBackupReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresShardedBackup> getResourceClass() {
    return StackGresShardedBackup.class;
  }
}
