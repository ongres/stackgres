/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Logger LOGGER = LoggerFactory.getLogger(GenericExceptionMapper.class);

  @Override
  public Response toResponse(Throwable throwable) {
    LOGGER.error("An error occurred in the REST API", throwable);

    int status = Status.INTERNAL_SERVER_ERROR.getStatusCode();
    if (throwable instanceof WebApplicationException) {
      status = WebApplicationException.class.cast(throwable).getResponse().getStatus();
    }

    return Response.status(status).type(MediaType.APPLICATION_JSON)
        .entity(ErrorResponse.create(throwable)).build();
  }
}
