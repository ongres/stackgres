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
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ValidationUtil.PGCONFIG_VALIDATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostgresConfigValidationResource extends AbstractValidationResource<PgConfigReview> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PostgresConfigValidationResource.class);

  @Inject
  public PostgresConfigValidationResource(ValidationPipeline<PgConfigReview> pipeline) {
    super(pipeline);
  }

  public PostgresConfigValidationResource() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Postgres configuration validation resource started");
  }

  /**
   * Admission Web hook callback.
   */
  @POST
  @Override
  public AdmissionReviewResponse validate(PgConfigReview admissionReview) {
    return super.validate(admissionReview);
  }
}
