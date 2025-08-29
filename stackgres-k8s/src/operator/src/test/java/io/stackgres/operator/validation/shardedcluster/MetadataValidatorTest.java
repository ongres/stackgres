/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import java.util.Map;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecAnnotations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecLabels;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.StackGresShardedClusterReviewBuilder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.Assertions;
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
  void doNothing_ifOperation_isNot_CreateOrUpdate() {
    enableRequestOperation(Operation.DELETE);
    try {
      validator.validate(review);
    } catch (ValidationFailed e) {
      Assertions.fail(e);
    }
  }

  @Test
  void doNothing_ifClusterLabels_areNull() {
    disableClusterLabels();
    disableClusterAnnotations();
    enableRequestOperation(Operation.CREATE);

    try {
      validator.validate(review);
    } catch (ValidationFailed e) {
      Assertions.fail(e);
    }
  }

  @Test
  void doNothing_ifClusterAnnotations_areNull() {
    disableClusterAnnotations();
    disableClusterLabels();
    enableRequestOperation(Operation.CREATE);

    try {
      validator.validate(review);
    } catch (ValidationFailed e) {
      Assertions.fail(e);
    }
  }

  @Test
  void catch_ValidationFailedException_ifClusterLabels_areWrong() {
    enableRequestOperation(Operation.CREATE);
    enableClusterLabels("kubernetes.io/connection-pooling", "true");

    try {
      validator.validate(review);
    } catch (ValidationFailed e) {
      Assertions.assertEquals(
                "The kubernetes.io/ and k8s.io/ prefixes are reserved for Kubernetes core components. "
                        + "But was kubernetes.io/connection-pooling", e.getMessage());
    }
  }

  @Test
  void catch_ValidationFailedException_ifClusterAnnotations_areWrong() {
    enableRequestOperation(Operation.CREATE);
    enableClusterAnnotations("k8s.io/fail-over", "true");

    try {
      validator.validate(review);
    } catch (ValidationFailed e) {
      Assertions.assertEquals(
              "The kubernetes.io/ and k8s.io/ prefixes are reserved for Kubernetes core components. "
                      + "But was k8s.io/fail-over", e.getMessage());
    }
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
    defaultCluster.getSpec().setMetadata(new StackGresShardedClusterSpecMetadata());
    defaultCluster.getSpec().getMetadata().setLabels(new StackGresShardedClusterSpecLabels());
    defaultCluster
            .getSpec()
            .getMetadata()
            .getLabels()
            .setServices(Map.of(key, value));

    defaultCluster
            .getSpec()
            .getMetadata()
            .getLabels()
            .setClusterPods(Map.of(key, value));
  }

  private void enableClusterAnnotations(String key, String value) {
    defaultCluster.getSpec().setMetadata(new StackGresShardedClusterSpecMetadata());
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresShardedClusterSpecAnnotations());
    defaultCluster
            .getSpec()
            .getMetadata()
            .getAnnotations()
            .setServices(Map.of(key, value));

    defaultCluster
            .getSpec()
            .getMetadata()
            .getAnnotations()
            .setClusterPods(Map.of(key, value));

    defaultCluster
            .getSpec()
            .getMetadata()
            .getAnnotations()
            .setAllResources(Map.of(key, value));

    defaultCluster
            .getSpec()
            .getMetadata()
            .getAnnotations()
            .setPrimaryService(Map.of(key, value));

    defaultCluster
            .getSpec()
            .getMetadata()
            .getAnnotations()
            .setReplicasService(Map.of(key, value));
  }
}
