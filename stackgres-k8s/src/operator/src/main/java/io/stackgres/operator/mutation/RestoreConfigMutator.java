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
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operatorframework.AdmissionReviewResponse;
import io.stackgres.operatorframework.JsonPatchMutationPipeline;
import io.stackgres.operatorframework.MutationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(MutationUtil.RESTORECONFIG_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RestoreConfigMutator implements MutationResource<RestoreConfigReview> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(BackupMutationResource.class);

  private JsonPatchMutationPipeline<RestoreConfigReview> pipeline;

  @Inject
  public RestoreConfigMutator(JsonPatchMutationPipeline<RestoreConfigReview> pipeline) {
    this.pipeline = pipeline;
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Restore configuration mutator resource started");
  }

  @Override
  @POST
  public AdmissionReviewResponse mutate(RestoreConfigReview admissionReview) {
    return mutate(admissionReview, pipeline);
  }
}
