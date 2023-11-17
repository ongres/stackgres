/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
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
