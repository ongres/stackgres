/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;

import com.google.common.base.Throwables;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.operator.mutation.MutationUtil;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.AdmissionResponse;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import org.jboss.resteasy.spi.ApplicationException;
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

    if (cause instanceof ApplicationException
        && cause.getCause() != null) {
      cause = cause.getCause();
    }

    if (statusCode == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
      LOGGER.error("An error occurred in the REST API", throwable);
    }

    String message = cause.getMessage();

    if (cause instanceof KubernetesClientException) {
      statusCode = Optional.ofNullable(
          KubernetesClientException.class.cast(cause).getStatus().getCode())
          .orElse(KubernetesClientException.class.cast(cause).getCode());
      message = KubernetesClientException.class.cast(cause).getStatus().getMessage();
    }

    if (uriInfo != null && (uriInfo.getPath().startsWith(ValidationUtil.VALIDATION_PATH + "/")
        || uriInfo.getPath().startsWith(MutationUtil.MUTATION_PATH + "/"))) {
      AdmissionResponse admissionResponse = new AdmissionResponse();
      admissionResponse.setAllowed(false);
      io.fabric8.kubernetes.api.model.Status status = new StatusBuilder()
          .withMessage(message)
          .withCode(statusCode)
          .build();
      admissionResponse.setStatus(status);
      AdmissionReviewResponse admissionReviewResponse = new AdmissionReviewResponse();
      admissionReviewResponse.setResponse(admissionResponse);
      return Response.ok().type(MediaType.APPLICATION_JSON)
          .entity(admissionReviewResponse).build();
    }

    return Response.status(statusCode).type(MediaType.APPLICATION_JSON)
        .entity(ErrorResponse.create(cause, message)).build();
  }

}
