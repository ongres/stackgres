/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistryValidatorTest {

  private RegistryValidator validator;

  @BeforeEach
  void setUp() {
    validator = new RegistryValidator();
  }

  private void setRegistryEnabled(StackGresCluster cluster, Boolean enabled) {
    StackGresClusterRegistry registry = new StackGresClusterRegistry();
    registry.setEnabled(enabled);
    cluster.getSpec().getConfigurations().setRegistry(registry);
  }

  @Test
  void givenCreation_shouldNotFail() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();
    setRegistryEnabled(review.getRequest().getObject(), true);

    validator.validate(review);
  }

  @Test
  void givenUnchangedRegistryUpdate_shouldNotFail() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadUpdate().get();
    setRegistryEnabled(review.getRequest().getObject(), true);
    setRegistryEnabled(review.getRequest().getOldObject(), true);

    validator.validate(review);
  }

  @Test
  void givenRegistryDisabledOnExistingClusterUpdate_shouldNotFail() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadUpdate().get();
    setRegistryEnabled(review.getRequest().getObject(), false);
    review.getRequest().getOldObject().getSpec().getConfigurations().setRegistry(null);

    validator.validate(review);
  }

  @Test
  void givenRegistryEnabledUpdate_shouldFail() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadUpdate().get();
    setRegistryEnabled(review.getRequest().getObject(), true);
    setRegistryEnabled(review.getRequest().getOldObject(), false);

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));

    assertEquals("registry enabled can not be changed", ex.getResult().getMessage());
  }

  @Test
  void givenRegistryDisabledUpdate_shouldFail() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadUpdate().get();
    setRegistryEnabled(review.getRequest().getObject(), false);
    setRegistryEnabled(review.getRequest().getOldObject(), true);

    assertThrows(ValidationFailed.class, () -> validator.validate(review));
  }

}
