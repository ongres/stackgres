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
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operatorframework.AdmissionReviewResponse;
import io.stackgres.operatorframework.JsonPatchMutationPipeline;
import io.stackgres.operatorframework.MutationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(MutationUtil.CLUSTER_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClusterMutationResource implements MutationResource<StackgresClusterReview> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SgPgBouncerMutationResource.class);

  private JsonPatchMutationPipeline<StackgresClusterReview> pipeline;

  @Inject
  public ClusterMutationResource(JsonPatchMutationPipeline<StackgresClusterReview> pipeline) {
    this.pipeline = pipeline;
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Cluster configuration mutator resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(StackgresClusterReview admissionReview) {
    return mutate(admissionReview, pipeline);
  }
}
