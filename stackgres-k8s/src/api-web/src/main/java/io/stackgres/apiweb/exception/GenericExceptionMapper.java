/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      GenericExceptionMapper.class);

  @Override
  public Response toResponse(Throwable throwable) {
    Throwable cause = Throwables.getRootCause(throwable);

    String message = cause.getMessage();

    int statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    LOGGER.error("An error occurred in the operator web api", throwable);

    return Response.status(statusCode).type(MediaType.APPLICATION_JSON)
        .entity(ErrorResponse.create(cause, message))
        .build();
  }
}
