/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import javax.ws.rs.core.Response;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.StatusDetailsBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.utils.JsonUtil;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KubernetesExceptionMapperTest {

  private KubernetesExceptionMapper mapper;

  final ConfigLoader context = new ConfigLoader();

  @BeforeEach
  void setUp() {

    final ResteasyUriInfo uriInfo = new ResteasyUriInfo(
        "http://localhost:8080/", "/stackgres/profile");
    mapper = new KubernetesExceptionMapper(uriInfo, context, new Kubernetes12StatusParser());

  }

  @Test
  void webHookErrorResponses_shouldBeParsed() {

    final String errorTypeUri = context.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);
    final String message = "StackGresProfile has invalid properties";
    final String field = "spec.memory";
    final String detail = "spec.memory cannot be empty";
    Status status = new StatusBuilder()
        .withReason(errorTypeUri)
        .withMessage(message)
        .withDetails(new StatusDetailsBuilder()
            .addNewCause(field, detail,
                "NotEmpty")
            .build())
        .withCode(400)
        .build();

    Response response = mapper.toResponse(new KubernetesClientException("error", 400, status));

    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

    assertEquals(errorTypeUri, errorResponse.getType());
    assertEquals(ErrorType.CONSTRAINT_VIOLATION.getTitle(), errorResponse.getTitle());
    assertEquals(detail, errorResponse.getDetail());
    assertEquals(field, errorResponse.getFields()[0]);

  }

  @Test
  void kubernetesValidations_shouldBeParsed() {

    Status status = JsonUtil.readFromJson("kube_status/status-1.13.12.json", Status.class);

    Response response = mapper.toResponse(new KubernetesClientException("error", 422, status));

    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

    assertEquals(context.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION), errorResponse.getType());
    assertEquals(ErrorType.CONSTRAINT_VIOLATION.getTitle(), errorResponse.getTitle());
    assertEquals("spec.memory in body should match '^[0-9]+(\\\\.[0-9]+)?(Mi|Gi)$'",
        errorResponse.getDetail());
    assertEquals("spec.memory", errorResponse.getFields()[0]);

  }

  @Test
  void kubernetes16Validations_shouldBeParsed() {


    mapper.setStatusParser(new Kubernetes16StatusParser());

    Status status = JsonUtil.readFromJson("kube_status/status-1.16.4.json", Status.class);

    Response response = mapper.toResponse(new KubernetesClientException("error", 422, status));

    ErrorResponse errorResponse = (ErrorResponse) response.getEntity();

    assertEquals(context.getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION), errorResponse.getType());
    assertEquals(ErrorType.CONSTRAINT_VIOLATION.getTitle(), errorResponse.getTitle());
    assertEquals("spec.memory in body should match '^[0-9]+(\\\\.[0-9]+)?(Mi|Gi)$'",
        errorResponse.getDetail());
    assertEquals("spec.memory", errorResponse.getFields()[0]);

  }
}