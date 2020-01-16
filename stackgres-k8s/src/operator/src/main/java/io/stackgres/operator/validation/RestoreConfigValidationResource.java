/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

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
import io.stackgres.operatorframework.ValidationPipeline;
import io.stackgres.operatorframework.ValidationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ValidationUtil.RESTORECONFIG_VALIDATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RestoreConfigValidationResource implements ValidationResource<RestoreConfigReview> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(RestoreConfigValidationResource.class);

  private ValidationPipeline<RestoreConfigReview> pipeline;

  @Inject
  public RestoreConfigValidationResource(ValidationPipeline<RestoreConfigReview> pipeline) {
    this.pipeline = pipeline;
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Restore validation resource started");
  }

  @Override
  @POST
  public AdmissionReviewResponse validate(RestoreConfigReview admissionReview) {
    return validate(admissionReview, pipeline);
  }
}
