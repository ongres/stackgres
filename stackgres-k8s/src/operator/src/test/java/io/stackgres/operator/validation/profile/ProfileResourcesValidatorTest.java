/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.common.crd.sgprofile.StackGresProfileHugePagesBuilder;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileResourcesValidatorTest {

  private ProfileResourcesValidator validator = new ProfileResourcesValidator();
  private SgProfileReview review;
  
  @BeforeEach
  public void setUp() {
    review = AdmissionReviewFixtures.instanceProfile().loadCreate().get();
  }

  @Test
  void validReview_shouldNotFail() throws ValidationFailed {
    validator.validate(review);
  }

  @Test
  void givenNegativeCpu_shouldFail() {
    review.getRequest().getObject().getSpec().setCpu("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.cpu");
  }

  @Test
  void givenNegativeMemory_shouldFail() {
    review.getRequest().getObject().getSpec().setMemory("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.memory");
  }

  @Test
  void givenNegativeHugePages2Mi_shouldFail() {
    review.getRequest().getObject().getSpec().setHugePages(
        new StackGresProfileHugePagesBuilder()
        .withHugepages2Mi("-1")
        .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.hugePages.hugepages-2Mi");
  }

  @Test
  void givenNegativeHugePages1Gi_shouldFail() {
    review.getRequest().getObject().getSpec().setHugePages(
        new StackGresProfileHugePagesBuilder()
        .withHugepages1Gi("-1")
        .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.hugePages.hugepages-1Gi");
  }

  @Test
  void givenNegativeContainerCpu_shouldFail() {
    review.getRequest().getObject().getSpec().getContainers()
        .get("pgbouncer").setCpu("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.containers.pgbouncer.cpu");
  }

  @Test
  void givenNegativeContainerMemory_shouldFail() {
    review.getRequest().getObject().getSpec().getContainers()
        .get("pgbouncer").setMemory("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.containers.pgbouncer.memory");
  }

  @Test
  void givenNegativeContainerHugePages2Mi_shouldFail() {
    review.getRequest().getObject().getSpec().getContainers()
        .get("pgbouncer").setHugePages(
            new StackGresProfileHugePagesBuilder()
            .withHugepages2Mi("-1")
            .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.containers.pgbouncer.hugePages.hugepages-2Mi");
  }

  @Test
  void givenNegativeContainerHugePages1Gi_shouldFail() {
    review.getRequest().getObject().getSpec().getContainers()
        .get("pgbouncer").setHugePages(
            new StackGresProfileHugePagesBuilder()
            .withHugepages1Gi("-1")
            .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.containers.pgbouncer.hugePages.hugepages-1Gi");
  }

  @Test
  void givenNegativeInitContainerCpu_shouldFail() {
    review.getRequest().getObject().getSpec().getInitContainers()
        .get("cluster-reconciliation-cycle").setCpu("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.initContainers.cluster-reconciliation-cycle.cpu");
  }

  @Test
  void givenNegativeInitContainerMemory_shouldFail() {
    review.getRequest().getObject().getSpec().getInitContainers()
        .get("cluster-reconciliation-cycle").setMemory("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.initContainers.cluster-reconciliation-cycle.memory");
  }

  @Test
  void givenNegativeInitContainerHugePages2Mi_shouldFail() {
    review.getRequest().getObject().getSpec().getInitContainers()
        .get("cluster-reconciliation-cycle").setHugePages(
            new StackGresProfileHugePagesBuilder()
            .withHugepages2Mi("-1")
            .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.initContainers.cluster-reconciliation-cycle.hugePages.hugepages-2Mi");
  }

  @Test
  void givenNegativeInitContainerHugePages1Gi_shouldFail() {
    review.getRequest().getObject().getSpec().getInitContainers()
        .get("cluster-reconciliation-cycle").setHugePages(
            new StackGresProfileHugePagesBuilder()
            .withHugepages1Gi("-1")
            .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.initContainers.cluster-reconciliation-cycle.hugePages.hugepages-1Gi");
  }

  @Test
  void givenNegativeRequestsCpu_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().setCpu("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.cpu");
  }

  @Test
  void givenNegativeRequestsMemory_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().setMemory("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.memory");
  }

  @Test
  void givenNegativeRequestsContainerCpu_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().getContainers()
        .get("pgbouncer").setCpu("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.containers.pgbouncer.cpu");
  }

  @Test
  void givenNegativeRequestsContainerMemory_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().getContainers()
        .get("pgbouncer").setMemory("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.containers.pgbouncer.memory");
  }

  @Test
  void givenNegativeRequestsContainerHugePages2Mi_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().getContainers()
        .get("pgbouncer").setHugePages(
            new StackGresProfileHugePagesBuilder()
            .withHugepages2Mi("-1")
            .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.containers.pgbouncer.hugePages.hugepages-2Mi");
  }

  @Test
  void givenNegativeRequestsContainerHugePages1Gi_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().getContainers()
        .get("pgbouncer").setHugePages(
            new StackGresProfileHugePagesBuilder()
            .withHugepages1Gi("-1")
            .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.containers.pgbouncer.hugePages.hugepages-1Gi");
  }

  @Test
  void givenNegativeRequestsInitContainerCpu_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().getInitContainers()
        .get("cluster-reconciliation-cycle").setCpu("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.initContainers.cluster-reconciliation-cycle.cpu");
  }

  @Test
  void givenNegativeRequestsInitContainerMemory_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().getInitContainers()
        .get("cluster-reconciliation-cycle").setMemory("-1");

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.initContainers.cluster-reconciliation-cycle.memory");
  }

  @Test
  void givenNegativeRequestsInitContainerHugePages2Mi_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().getInitContainers()
        .get("cluster-reconciliation-cycle").setHugePages(
            new StackGresProfileHugePagesBuilder()
            .withHugepages2Mi("-1")
            .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.initContainers.cluster-reconciliation-cycle.hugePages.hugepages-2Mi");
  }

  @Test
  void givenNegativeRequestsInitContainerHugePages1Gi_shouldFail() {
    review.getRequest().getObject().getSpec().getRequests().getInitContainers()
        .get("cluster-reconciliation-cycle").setHugePages(
            new StackGresProfileHugePagesBuilder()
            .withHugepages1Gi("-1")
            .build());

    checkValidationException(
        "Quantity can not be negative, but was -1",
        "spec.requests.initContainers.cluster-reconciliation-cycle.hugePages.hugepages-1Gi");
  }

  void checkValidationException(String message, String field) {
    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));
    Assert.assertEquals(ex.getResult().getMessage(), message);
    Assert.assertTrue(ex.getResult().getReason().endsWith("/api/responses/error#constraint-violation"));
    Assert.assertEquals(ex.getResult().getDetails().getCauses().size(), 1);
    Assert.assertEquals(ex.getResult().getDetails().getCauses().get(0).getField(), field);
  }
}
