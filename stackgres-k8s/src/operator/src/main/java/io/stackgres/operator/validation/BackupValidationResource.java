/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.operatorframework.AdmissionReviewResponse;
import io.stackgres.operatorframework.ValidationPipeline;
import io.stackgres.operatorframework.ValidationResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ValidationUtil.BACKUP_VALIDATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackupValidationResource implements ValidationResource<BackupReview> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BackupValidationResource.class);

  private ValidationPipeline<BackupReview> pipeline;

  @Inject
  public BackupValidationResource(@Any ValidationPipeline<BackupReview> pipeline) {
    this.pipeline = pipeline;
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Backup validation resource started");
  }

  @Override
  @POST
  public AdmissionReviewResponse validate(BackupReview admissionReview) {
    return validate(admissionReview, pipeline);
  }

}
