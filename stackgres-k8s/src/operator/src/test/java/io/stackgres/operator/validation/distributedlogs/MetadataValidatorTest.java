/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.distributedlogs;

import java.util.Map;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecAnnotations;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecMetadata;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.StackGresDistributedLogsReviewBuilder;
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

  StackGresDistributedLogs defaultCluster;

  StackGresDistributedLogsReview review;

  @BeforeEach
  void setUp() {
    validator = new MetadataValidator();

    defaultCluster = Fixtures.distributedLogs().loadDefault().get();
    review = new StackGresDistributedLogsReviewBuilder().withRequest(new AdmissionRequest<>()).build();
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
  void doNothing_ifClusterAnnotations_areNull() {
    disableClusterAnnotations();
    enableRequestOperation(Operation.CREATE);

    try {
      validator.validate(review);
    } catch (ValidationFailed e) {
      Assertions.fail(e);
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

  private void disableClusterAnnotations() {
    defaultCluster.getSpec().setMetadata(new StackGresDistributedLogsSpecMetadata());
    defaultCluster.getSpec().getMetadata().setAnnotations(null);
  }

  private void enableClusterAnnotations(String key, String value) {
    defaultCluster.getSpec().setMetadata(new StackGresDistributedLogsSpecMetadata());
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresDistributedLogsSpecAnnotations());
    defaultCluster
            .getSpec()
            .getMetadata()
            .getAnnotations()
            .setServices(Map.of(key, value));

    defaultCluster
            .getSpec()
            .getMetadata()
            .getAnnotations()
            .setPods(Map.of(key, value));

    defaultCluster
            .getSpec()
            .getMetadata()
            .getAnnotations()
            .setAllResources(Map.of(key, value));
  }

}
