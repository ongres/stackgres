/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;

import com.google.common.base.Throwables;

import io.stackgres.operator.mutation.MutationUtil;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.AdmissionResponse;
import io.stackgres.operatorframework.AdmissionReviewResponse;
import io.stackgres.operatorframework.Result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractGenericExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      AbstractGenericExceptionMapper.class);

  @Context
  private UriInfo uriInfo;

  @Override
  public Response toResponse(T throwable) {
    int status = Status.INTERNAL_SERVER_ERROR.getStatusCode();
    Throwable cause = Throwables.getRootCause(throwable);
    if (cause instanceof WebApplicationException) {
      status = WebApplicationException.class.cast(cause).getResponse().getStatus();
    }

    if (status == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
      LOGGER.error("An error occurred in the REST API", throwable);
    }

    if (uriInfo != null && (uriInfo.getPath().startsWith(ValidationUtil.VALIDATION_PATH + "/")
        || uriInfo.getPath().startsWith(MutationUtil.MUTATION_PATH + "/"))) {
      AdmissionResponse admissionResponse = new AdmissionResponse();
      admissionResponse.setAllowed(false);
      admissionResponse.setStatus(new Result(status, throwable.getMessage()));
      AdmissionReviewResponse admissionReviewResponse = new AdmissionReviewResponse();
      admissionReviewResponse.setResponse(admissionResponse);
      return Response.ok().type(MediaType.APPLICATION_JSON)
          .entity(admissionReviewResponse).build();
    }

    return Response.status(status).type(MediaType.APPLICATION_JSON)
        .entity(ErrorResponse.create(throwable)).build();
  }

}
