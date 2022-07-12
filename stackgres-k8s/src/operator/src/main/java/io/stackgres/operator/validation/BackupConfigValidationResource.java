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
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ValidationUtil.BACKUPCONFIG_VALIDATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackupConfigValidationResource implements ValidationResource<BackupConfigReview> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(BackupConfigValidationResource.class);

  private ValidationPipeline<BackupConfigReview> validationPipeline;

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Backup configuration validation resource started");
  }

  /**
   * Admission Web hook callback.
   */
  @POST
  @Override
  public AdmissionReviewResponse validate(BackupConfigReview admissionReview) {
    return validate(admissionReview, validationPipeline);
  }

  @Inject
  public void setValidationPipeline(
      @Any ValidationPipeline<BackupConfigReview> validationPipeline) {
    this.validationPipeline = validationPipeline;
  }
}
