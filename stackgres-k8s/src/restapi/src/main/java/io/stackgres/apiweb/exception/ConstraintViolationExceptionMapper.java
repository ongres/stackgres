/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.exception;

import java.util.stream.Collectors;

import io.stackgres.common.ErrorType;
import io.stackgres.common.validation.ValidationUtil;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConstraintViolationExceptionMapper
    implements ExceptionMapper<ConstraintViolationException> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

  public static final int UNPROCESSABLE_ENTITY_STATUS = 422;

  @Override
  public Response toResponse(ConstraintViolationException e) {
    LOGGER.debug("Error on request", e);

    LOGGER.debug("Parsing ConstraintViolationException validation error");
    ErrorType errorType = ErrorType.CONSTRAINT_VIOLATION;

    final String detail = !e.getConstraintViolations().isEmpty()
        ? Seq.seq(e.getConstraintViolations()).distinct()
            .map(violation -> violation.getMessage())
            .collect(Collectors.joining("\n"))
        : e.getMessage();

    ErrorResponse response =
        new ErrorResponseBuilder(ErrorType.getErrorTypeUri(errorType))
        .setTitle(errorType.getTitle())
        .setDetail(detail)
        .setStatus(UNPROCESSABLE_ENTITY_STATUS)
        .setFields(e.getConstraintViolations().stream()
            .flatMap(violation -> ValidationUtil.getOffendingFields(
                violation.getRootBean(), violation).stream())
            .toArray(String[]::new))
        .build();

    return Response.status(UNPROCESSABLE_ENTITY_STATUS)
        .type(MediaType.APPLICATION_JSON)
        .entity(response).build();
  }

}
