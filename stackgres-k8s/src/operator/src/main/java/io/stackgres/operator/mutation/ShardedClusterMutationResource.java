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
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(MutationUtil.SHARDED_CLUSTER_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShardedClusterMutationResource
    implements MutationResource<StackGresShardedClusterReview> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedClusterMutationResource.class);

  private JsonPatchMutationPipeline<StackGresShardedClusterReview> pipeline;

  @Inject
  public void setPipeline(JsonPatchMutationPipeline<StackGresShardedClusterReview> pipeline) {
    this.pipeline = pipeline;
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("ShardedCluster mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(StackGresShardedClusterReview admissionReview) {
    return mutate(admissionReview, pipeline);
  }
}
