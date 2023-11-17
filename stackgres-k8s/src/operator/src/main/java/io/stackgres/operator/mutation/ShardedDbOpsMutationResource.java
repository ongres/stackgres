/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.common.ShardedDbOpsReview;
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

@Path(MutationUtil.SHARDED_DBOPS_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShardedDbOpsMutationResource
    extends AbstractMutationResource<StackGresShardedDbOps, ShardedDbOpsReview> {

  @Inject
  public ShardedDbOpsMutationResource(
      ObjectMapper objectMapper,
      MutationPipeline<StackGresShardedDbOps, ShardedDbOpsReview> pipeline) {
    super(objectMapper, pipeline);
  }

  public ShardedDbOpsMutationResource() {
    super(null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    getLogger().info("Sharded DbOps mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(ShardedDbOpsReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresShardedDbOps> getResourceClass() {
    return StackGresShardedDbOps.class;
  }
}
