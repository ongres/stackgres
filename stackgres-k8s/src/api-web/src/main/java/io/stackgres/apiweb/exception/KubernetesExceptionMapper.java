/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.exception;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.apiweb.rest.utils.Kubernetes16StatusParser;
import io.stackgres.apiweb.rest.utils.StatusParser;
import io.stackgres.common.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesExceptionMapper implements ExceptionMapper<KubernetesClientException> {

  private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesExceptionMapper.class);

  // Status 422 not present in javax.ws.rs.core.Response.Status
  public static final int UNPROCESSABLE_ENTITY_STATUS = 422;

  public static final int CONFLICT_STATUS = 409;

  private StatusParser statusParser;

  public KubernetesExceptionMapper(StatusParser statusParser) {
    this.statusParser = statusParser;
  }

  @Override
  public Response toResponse(KubernetesClientException e) {
    LOGGER.debug("Error on request", e);

    final Status status = e.getStatus();

    String reason = status.getReason();

    if (reason != null && ErrorType.isDocumentationUri(reason)) {
      return toErrorTypeResponse(status, reason);
    }

    return toResponse(status);

  }

  private Response toResponse(Status status) {

    if (status.getCode() == CONFLICT_STATUS) {
      LOGGER.debug("Kubernetes responded with Conflict status. Parsing response");
      String type = ErrorType.getErrorTypeUri(ErrorType.ALREADY_EXISTS);
      String title = ErrorType.ALREADY_EXISTS.getTitle();
      String detail = statusParser.parseDetails(status);
      String[] fields = statusParser.parseFields(status);

      ErrorResponse response = new ErrorResponseBuilder(type)
          .setStatus(status.getCode())
          .setTitle(title)
          .setDetail(new String(JsonStringEncoder.getInstance().quoteAsString(detail)))
          .setFields(fields)
          .build();

      return Response.status(status.getCode()).type(MediaType.APPLICATION_JSON)
          .entity(response).build();
    }

    if (status.getCode() == UNPROCESSABLE_ENTITY_STATUS) {
      LOGGER.debug("Kubernetes responded with Unprocessable entity status. Parsing response");
      String type = ErrorType.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);
      String title = ErrorType.CONSTRAINT_VIOLATION.getTitle();
      String detail = statusParser.parseDetails(status);
      String[] fields = statusParser.parseFields(status);

      ErrorResponse response = new ErrorResponseBuilder(type)
          .setStatus(status.getCode())
          .setTitle(title)
          .setDetail(new String(JsonStringEncoder.getInstance().quoteAsString(detail)))
          .setFields(fields)
          .build();

      return Response.status(status.getCode()).type(MediaType.APPLICATION_JSON)
          .entity(response).build();
    }

    if (status.getCode() == javax.ws.rs.core.Response.Status.FORBIDDEN.getStatusCode()) {
      LOGGER.debug("Kubernetes responded with FORBIDDEN status. Parsing response");
      String type = ErrorType.getErrorTypeUri(ErrorType.FORBIDDEN_AUTHORIZATION);
      String title = ErrorType.FORBIDDEN_AUTHORIZATION.getTitle();
      String detail = status.getMessage();

      ErrorResponse response = new ErrorResponseBuilder(type)
          .setStatus(status.getCode())
          .setTitle(title)
          .setDetail(new String(JsonStringEncoder.getInstance().quoteAsString(detail)))
          .build();

      return Response.status(status.getCode()).type(MediaType.APPLICATION_JSON)
          .entity(response).build();
    }

    LOGGER.debug("Parsing unexpected error response from kubernetes");
    List<String> fields = new ArrayList<>();
    List<String> fieldCauses = new ArrayList<>();

    if (status.getDetails() != null) {
      final List<StatusCause> causes = status.getDetails().getCauses();
      if (causes != null) {
        causes.forEach(c -> {
          fields.add(c.getField());
          fieldCauses.add("· " + Kubernetes16StatusParser.cleanupMessage(c.getMessage()));
        });
      }
    }

    ErrorResponse errorResponse = new ErrorResponseBuilder(status.getKind())
        .setTitle(status.getReason())
        .setDetail(String.join("\n", fieldCauses))
        .setFields(fields.toArray(new String[0]))
        .build();

    return Response.status(status.getCode()).type(MediaType.APPLICATION_JSON)
        .entity(errorResponse).build();
  }

  private Response toErrorTypeResponse(Status status, String reason) {
    LOGGER.debug("Parsing StackGres validation error");
    ErrorType errorType = ErrorType.parseErrorType(reason);

    List<String> fields = new ArrayList<>();
    List<String> fieldCauses = new ArrayList<>();

    if (status.getDetails() != null) {
      final List<StatusCause> causes = status.getDetails().getCauses();
      if (causes != null) {
        causes.forEach(c -> {
          fields.add(c.getField());
          fieldCauses.add("· " + Kubernetes16StatusParser.cleanupMessage(c.getMessage()));
        });
      }
    }

    final String detail = !fieldCauses.isEmpty()
        ? String.join("\n", fieldCauses)
        : status.getMessage();

    ErrorResponse response = new ErrorResponseBuilder(reason)
        .setTitle(errorType.getTitle())
        .setDetail(detail)
        .setStatus(status.getCode())
        .setFields(fields.toArray(new String[0]))
        .build();

    return Response.status(status.getCode()).type(MediaType.APPLICATION_JSON)
        .entity(response).build();
  }

  public void setStatusParser(StatusParser statusParser) {
    this.statusParser = statusParser;
  }
}
