/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import io.quarkus.security.UnauthorizedException;

public class UnauthorizedExceptionMapper
    implements ExceptionMapper<UnauthorizedException> {

  @Override
  public Response toResponse(UnauthorizedException throwable) {
    return Response.status(Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON)
        .entity(ErrorResponse.create(throwable, "Not authorized")).build();
  }

}
