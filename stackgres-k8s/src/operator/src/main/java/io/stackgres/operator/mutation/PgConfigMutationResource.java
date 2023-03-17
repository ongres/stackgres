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
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;

@Path(MutationUtil.PGCONFIG_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PgConfigMutationResource
    extends MutationResource<StackGresPostgresConfig, PgConfigReview> {

  @Inject
  public PgConfigMutationResource(
      MutationPipeline<StackGresPostgresConfig, PgConfigReview> pipeline) {
    super(pipeline);
  }

  public PgConfigMutationResource() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    getLogger().info("Postgres configuration mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(PgConfigReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresPostgresConfig> getResourceClass() {
    return StackGresPostgresConfig.class;
  }
}
