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
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.common.ConfigReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;

@Path(MutationUtil.CONFIG_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfigMutationResource
    extends AbstractMutationResource<StackGresConfig, ConfigReview> {

  @Inject
  public ConfigMutationResource(
      ObjectMapper objectMapper,
      MutationPipeline<StackGresConfig, ConfigReview> pipeline) {
    super(objectMapper, pipeline);
  }

  public ConfigMutationResource() {
    super(null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    getLogger().info("Config mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(ConfigReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresConfig> getResourceClass() {
    return StackGresConfig.class;
  }
}
