/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.mutation.MutationUtil;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.AdmissionResponse;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;

public class KubernetesExceptionMapper implements ExceptionMapper<KubernetesClientException> {

  //Status is not present in javax.ws.rs.core.Response.Status
  public static final int UNPROCESSABLE_ENTITY_STATUS = 422;

  private UriInfo uriInfo;

  private ConfigContext context;

  private StatusParser statusParser;

  public KubernetesExceptionMapper(UriInfo uriInfo,
                                   ConfigContext context,
                                   StatusParser statusParser) {
    this.uriInfo = uriInfo;
    this.context = context;
    this.statusParser = statusParser;
  }

  @Override
  public Response toResponse(KubernetesClientException e) {

    final Status status = e.getStatus();
    if (uriInfo != null && (uriInfo.getPath().startsWith(ValidationUtil.VALIDATION_PATH + "/")
        || uriInfo.getPath().startsWith(MutationUtil.MUTATION_PATH + "/"))) {
      return toAdmissionResponse(status);
    }
    String reason = status.getReason();

    if (reason != null && context.isDocumentationUri(reason)) {

      return toErrorTypeResponse(status, reason);
    }

    return toResponse(status);

  }

  private Response toResponse(Status status) {

    if (status.getCode() == UNPROCESSABLE_ENTITY_STATUS) {
      String type = context.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);
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
    List<String> fields = new ArrayList<>();
    List<String> fieldCauses = new ArrayList<>();

    if (status.getDetails() != null) {
      final List<StatusCause> causes = status.getDetails().getCauses();
      if (causes != null) {
        causes.forEach(c -> {
          fields.add(c.getField());
          fieldCauses.add(c.getMessage());
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
    ErrorType errorType = context.parseErrorType(reason);

    List<String> fields = new ArrayList<>();
    List<String> fieldCauses = new ArrayList<>();

    if (status.getDetails() != null) {
      final List<StatusCause> causes = status.getDetails().getCauses();
      if (causes != null) {
        causes.forEach(c -> {
          fields.add(c.getField());
          fieldCauses.add(c.getMessage());
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

  private Response toAdmissionResponse(Status status) {
    AdmissionResponse admissionResponse = new AdmissionResponse();
    admissionResponse.setAllowed(false);
    admissionResponse.setStatus(status);
    AdmissionReviewResponse admissionReviewResponse = new AdmissionReviewResponse();
    admissionReviewResponse.setResponse(admissionResponse);

    return Response.ok().type(MediaType.APPLICATION_JSON)
        .entity(admissionReviewResponse).build();
  }

  public void setStatusParser(StatusParser statusParser) {
    this.statusParser = statusParser;
  }
}
