/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.Map;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotationsBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabelsBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.StackGresClusterReviewBuilder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataValidatorTest {

  MetadataValidator validator;

  StackGresCluster defaultCluster;
  StackGresClusterReview review;

  @BeforeEach
  void setUp() {
    validator = new MetadataValidator();

    defaultCluster = Fixtures.cluster().loadDefault().get();
    review = new StackGresClusterReviewBuilder().withRequest(new AdmissionRequest<>()).build();
    review.getRequest().setObject(defaultCluster);
  }

  @Test
  void doNothing_ifOperation_isNot_CreateOrUpdate() throws Exception {
    enableRequestOperation(Operation.DELETE);
    validator.validate(review);
  }

  @Test
  void doNothing_ifClusterLabels_areNull() throws Exception {
    disableClusterLabels();
    disableClusterAnnotations();
    enableRequestOperation(Operation.CREATE);

    validator.validate(review);
  }

  @Test
  void doNothing_ifClusterAnnotations_areNull() throws Exception {
    disableClusterAnnotations();
    disableClusterLabels();
    enableRequestOperation(Operation.CREATE);

    validator.validate(review);
  }

  @Test
  void catch_ValidationFailedException_ifClusterLabels_areWrong() throws Exception {
    enableRequestOperation(Operation.CREATE);
    disableClusterAnnotations();
    enableClusterLabels("kubernetes.io/connection-pooling", "true");

    validator.validate(review);
  }

  @Test
  void catch_ValidationFailedException_ifClusterAnnotations_areWrong() throws Exception {
    enableRequestOperation(Operation.CREATE);
    enableClusterAnnotations("k8s.io/fail-over", "true");

    validator.validate(review);
  }

  private void enableRequestOperation(Operation operation) {
    review.getRequest().setOperation(operation);
  }

  private void disableClusterLabels() {
    defaultCluster.getSpec().getMetadata().setLabels(null);
  }

  private void disableClusterAnnotations() {
    defaultCluster.getSpec().getMetadata().setAnnotations(null);
  }

  private void enableClusterLabels(String key, String value) {
    defaultCluster
        .getSpec()
        .getMetadata()
        .setLabels(
            new StackGresClusterSpecLabelsBuilder()
            .withAllResources(Map.of(key, value))
            .withServices(Map.of(key, value))
            .withPrimaryService(Map.of(key, value))
            .withReplicasService(Map.of(key, value))
            .withClusterPods(Map.of(key, value))
            .withServiceAccount(Map.of(key, value))
            .build());
  }

  private void enableClusterAnnotations(String key, String value) {
    defaultCluster
          .getSpec()
          .getMetadata()
          .setAnnotations(
              new StackGresClusterSpecAnnotationsBuilder()
              .withAllResources(Map.of(key, value))
              .withServices(Map.of(key, value))
              .withPrimaryService(Map.of(key, value))
              .withReplicasService(Map.of(key, value))
              .withClusterPods(Map.of(key, value))
              .withServiceAccount(Map.of(key, value))
              .build());
  }

}
