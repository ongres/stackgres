/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import com.google.common.base.Throwables;
import io.quarkus.resteasy.runtime.UnauthorizedExceptionMapper;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractGenericExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      AbstractGenericExceptionMapper.class);

  @Context
  protected UriInfo uriInfo;

  @Override
  public Response toResponse(T throwable) {
    int statusCode = Status.INTERNAL_SERVER_ERROR.getStatusCode();
    Throwable cause = Throwables.getRootCause(throwable);
    if (cause instanceof WebApplicationException) {
      statusCode = WebApplicationException.class.cast(cause).getResponse().getStatus();
    }
    if (cause instanceof UnauthorizedException) {
      return new UnauthorizedExceptionMapper().toResponse(UnauthorizedException.class.cast(cause));
    }

    if (statusCode == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
      LOGGER.error("An error occurred in the REST API", throwable);
    }

    String message = cause.getMessage();

    return Response.status(statusCode).type(MediaType.APPLICATION_JSON)
        .entity(message)
        .build();
  }

}
