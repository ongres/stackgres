/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.UUID;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/stackgres/validation/pgconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostgresConfigValidationResource {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PostgresConfigValidationResource.class);

  private ValidationPipeline<PgConfigReview> validationPipeline;

  @Inject
  public PostgresConfigValidationResource(ValidationPipeline<PgConfigReview> validationPipeline) {
    this.validationPipeline = validationPipeline;
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Postgres configuration validation resource started");
  }

  /**
   * Admission Web hook callback.
   */
  @POST
  public AdmissionReviewResponse validate(PgConfigReview admissionReview) {
    AdmissionRequest<StackGresPostgresConfig> request = admissionReview.getRequest();

    UUID requestUid = request.getUid();
    LOGGER.info("Validating admission review " + requestUid.toString()
        + " of kind " + request.getKind().toString());
    AdmissionResponse response = new AdmissionResponse();
    response.setUid(requestUid);

    AdmissionReviewResponse reviewResponse = new AdmissionReviewResponse();
    reviewResponse.setResponse(response);

    try {

      reviewResponse.setGroup(admissionReview.getGroup());
      reviewResponse.setKind(admissionReview.getKind());
      reviewResponse.setVersion(admissionReview.getVersion());
      validationPipeline.validate(admissionReview);
      response.setAllowed(true);
    } catch (ValidationFailed vfex) {
      LOGGER.error("Cannot proceed with request " + requestUid.toString()
          + ", validation failed ", vfex);
      response.setAllowed(false);
      response.setStatus(vfex.getResult());
    } catch (Exception ex) {
      LOGGER.error("Cannot complete the validation pipeline due to some unexpected error: ", ex);
      Result status = new Result(500, ex.getMessage());
      response.setAllowed(false);
      response.setStatus(status);
    }

    return reviewResponse;

  }
}
