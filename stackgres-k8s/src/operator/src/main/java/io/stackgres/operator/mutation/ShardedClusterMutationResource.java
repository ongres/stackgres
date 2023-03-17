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
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;

@Path(MutationUtil.SHARDED_CLUSTER_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShardedClusterMutationResource
    extends MutationResource<StackGresShardedCluster, StackGresShardedClusterReview> {

  @Inject
  public ShardedClusterMutationResource(
      MutationPipeline<StackGresShardedCluster, StackGresShardedClusterReview> pipeline) {
    super(pipeline);
  }

  public ShardedClusterMutationResource() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    getLogger().info("ShardedCluster mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(StackGresShardedClusterReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresShardedCluster> getResourceClass() {
    return StackGresShardedCluster.class;
  }
}
