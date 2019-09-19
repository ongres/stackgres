/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Path("/validation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ValidationResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationResource.class);

  @POST
  public AdmissionResponse validate(AdmissionReview cluster) {

    UUID requestUid = cluster.getRequest().getUid();
    LOGGER.info("Validating admission review " + requestUid.toString());

    AdmissionResponse response = new AdmissionResponse();
    response.setUid(requestUid);
    response.setAllowed(true);
    return response;

  }

}
