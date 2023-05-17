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
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;

@Path(MutationUtil.PROFILE_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SgProfileMutationResource
    extends MutationResource<StackGresProfile, SgProfileReview> {

  @Inject
  public SgProfileMutationResource(
      ObjectMapper objectMapper,
      MutationPipeline<StackGresProfile, SgProfileReview> pipeline) {
    super(objectMapper, pipeline);
  }

  public SgProfileMutationResource() {
    super(null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    getLogger().info("SgProfile configuration mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(SgProfileReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresProfile> getResourceClass() {
    return StackGresProfile.class;
  }
}
