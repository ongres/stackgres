/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/validation/pgconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostgresConfigValidationResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterValidationResource.class);

  /**
   * Admission Web hook callback.
   */
  @POST
  public AdmissionReviewResponse validate(PgConfigReview admissionReview)
      throws JsonProcessingException {
    AdmissionRequest<StackGresPostgresConfig> request = admissionReview.getRequest();

    UUID requestUid = request.getUid();
    LOGGER.info("Validating admission review " + requestUid.toString()
        + " of kind " + request.getKind().toString());
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(admissionReview);
    LOGGER.info("validating: " + json);

    AdmissionResponse response = new AdmissionResponse();
    response.setUid(requestUid);

    AdmissionReviewResponse reviewResponse = new AdmissionReviewResponse();
    reviewResponse.setResponse(response);

    reviewResponse.setGroup(admissionReview.getGroup());
    reviewResponse.setKind(admissionReview.getKind());
    reviewResponse.setVersion(admissionReview.getVersion());

    response.setAllowed(true);

    return reviewResponse;

  }
}
