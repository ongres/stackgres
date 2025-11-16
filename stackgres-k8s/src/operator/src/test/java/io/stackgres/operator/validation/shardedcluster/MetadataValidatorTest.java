/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Map;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadataBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.StackGresShardedClusterReviewBuilder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataValidatorTest {

  MetadataValidator validator;

  StackGresShardedCluster defaultCluster;

  StackGresShardedClusterReview review;

  @BeforeEach
  void setUp() {
    validator = new MetadataValidator();

    defaultCluster = Fixtures.shardedCluster().loadDefault().get();
    review = new StackGresShardedClusterReviewBuilder().withRequest(new AdmissionRequest<>()).build();
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
    defaultCluster.getSpec().setMetadata(new StackGresShardedClusterSpecMetadata());
    defaultCluster.getSpec().getMetadata().setLabels(null);
  }

  private void disableClusterAnnotations() {
    defaultCluster.getSpec().setMetadata(new StackGresShardedClusterSpecMetadata());
    defaultCluster.getSpec().getMetadata().setAnnotations(null);
  }

  private void enableClusterLabels(String key, String value) {
    defaultCluster.getSpec().setMetadata(
        new StackGresShardedClusterSpecMetadataBuilder()
        .withNewLabels()
        .withAllResources(Map.of(key, value))
        .withServices(Map.of(key, value))
        .withPrimaryService(Map.of(key, value))
        .withReplicasService(Map.of(key, value))
        .withCoordinatorPrimaryService(Map.of(key, value))
        .withCoordinatorAnyService(Map.of(key, value))
        .withShardsPrimariesService(Map.of(key, value))
        .withClusterPods(Map.of(key, value))
        .withServiceAccount(Map.of(key, value))
        .endLabels()
        .build());
  }

  private void enableClusterAnnotations(String key, String value) {
    defaultCluster.getSpec().setMetadata(
        new StackGresShardedClusterSpecMetadataBuilder()
        .withNewAnnotations()
        .withAllResources(Map.of(key, value))
        .withServices(Map.of(key, value))
        .withPrimaryService(Map.of(key, value))
        .withReplicasService(Map.of(key, value))
        .withCoordinatorPrimaryService(Map.of(key, value))
        .withCoordinatorAnyService(Map.of(key, value))
        .withShardsPrimariesService(Map.of(key, value))
        .withClusterPods(Map.of(key, value))
        .withServiceAccount(Map.of(key, value))
        .endAnnotations()
        .build());
  }
}
