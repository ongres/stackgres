/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.stackgres.operator.validation.validators.ValidationFailed;
import io.stackgres.operator.validation.validators.ValidationPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/validation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ValidationResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationResource.class);

  @Inject
  private ValidationPipeline pipeline;

  @POST
  public AdmissionResponse validate(AdmissionReview cluster) {

    UUID requestUid = cluster.getRequest().getUid();
    LOGGER.info("Validating admission review " + requestUid.toString());

    AdmissionResponse response = new AdmissionResponse();
    response.setUid(requestUid);

    try {
      pipeline.validator(cluster);
      response.setAllowed(true);
    } catch (ValidationFailed validationFailed) {
      Result result = validationFailed.getResult();
      LOGGER.error("cannot proceed with request "
          + requestUid.toString() + " cause: " + result.getMessage());
      response.setAllowed(false);
      response.setStatus(result);
    }

    return response;

  }

}
