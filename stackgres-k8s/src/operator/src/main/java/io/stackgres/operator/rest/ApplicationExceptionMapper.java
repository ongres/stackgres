/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Throwables;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.operatorframework.admissionwebhook.AdmissionResponse;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import org.jboss.resteasy.spi.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ApplicationExceptionMapper.class);

  @Override
  public Response toResponse(ApplicationException e) {
    Throwable cause = Throwables.getRootCause(e);

    if (cause instanceof KubernetesClientException) {
      AdmissionResponse admissionResponse = new AdmissionResponse();
      admissionResponse.setAllowed(false);
      admissionResponse.setStatus(((KubernetesClientException) cause).getStatus());
      AdmissionReviewResponse admissionReviewResponse = new AdmissionReviewResponse();
      admissionReviewResponse.setResponse(admissionResponse);
      return Response.ok().type(MediaType.APPLICATION_JSON)
          .entity(admissionReviewResponse).build();
    }

    int statusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    if (cause instanceof WebApplicationException) {
      statusCode = ((WebApplicationException) cause).getResponse().getStatus();
    }

    if (statusCode == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
      LOGGER.error("An error occurred in the operator web api", e);
    }

    String message = cause.getMessage();

    return Response.status(statusCode).type(MediaType.APPLICATION_JSON)
        .entity(message)
        .build();
  }

}
