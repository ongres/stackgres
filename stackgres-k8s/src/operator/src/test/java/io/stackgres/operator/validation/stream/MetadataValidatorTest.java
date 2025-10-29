/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.stream;

import java.util.Map;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSpecAnnotationsBuilder;
import io.stackgres.common.crd.sgstream.StackGresStreamSpecLabelsBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.common.StackGresStreamReviewBuilder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataValidatorTest {

  MetadataValidator validator;

  StackGresStream defaultStream;
  StackGresStreamReview review;

  @BeforeEach
  void setUp() {
    validator = new MetadataValidator();

    defaultStream = Fixtures.stream().loadSgClusterToCloudEvent().getBuilder()
        .editSpec()
        .withNewMetadata()
        .endMetadata()
        .endSpec()
        .build();
    review = new StackGresStreamReviewBuilder().withRequest(new AdmissionRequest<>()).build();
    review.getRequest().setObject(defaultStream);
  }

  @Test
  void doNothing_ifOperation_isNot_CreateOrUpdate() throws Exception {
    enableRequestOperation(Operation.DELETE);
    validator.validate(review);
  }

  @Test
  void doNothing_ifStreamLabels_areNull() throws Exception {
    disableStreamLabels();
    disableStreamAnnotations();
    enableRequestOperation(Operation.CREATE);

    validator.validate(review);
  }

  @Test
  void doNothing_ifStreamAnnotations_areNull() throws Exception {
    disableStreamAnnotations();
    disableStreamLabels();
    enableRequestOperation(Operation.CREATE);

    validator.validate(review);
  }

  @Test
  void catch_ValidationFailedException_ifStreamLabels_areWrong() throws Exception {
    enableRequestOperation(Operation.CREATE);
    disableStreamAnnotations();
    enableStreamLabels("kubernetes.io/connection-pooling", "true");

    validator.validate(review);
  }

  @Test
  void catch_ValidationFailedException_ifStreamAnnotations_areWrong() throws Exception {
    enableRequestOperation(Operation.CREATE);
    enableStreamAnnotations("k8s.io/fail-over", "true");

    validator.validate(review);
  }

  private void enableRequestOperation(Operation operation) {
    review.getRequest().setOperation(operation);
  }

  private void disableStreamLabels() {
    defaultStream.getSpec().getMetadata().setLabels(null);
  }

  private void disableStreamAnnotations() {
    defaultStream.getSpec().getMetadata().setAnnotations(null);
  }

  private void enableStreamLabels(String key, String value) {
    defaultStream
        .getSpec()
        .getMetadata()
        .setLabels(
            new StackGresStreamSpecLabelsBuilder()
            .withAllResources(Map.of(key, value))
            .withPods(Map.of(key, value))
            .withServiceAccount(Map.of(key, value))
            .build());
  }

  private void enableStreamAnnotations(String key, String value) {
    defaultStream
          .getSpec()
          .getMetadata()
          .setAnnotations(
              new StackGresStreamSpecAnnotationsBuilder()
              .withAllResources(Map.of(key, value))
              .withPods(Map.of(key, value))
              .withServiceAccount(Map.of(key, value))
              .build());
  }

}
