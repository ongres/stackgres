/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniInitialConfig;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniInitialConfigValidatorTest {

  private PatroniInitialConfigValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PatroniInitialConfigValidator();
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();

    validator.validate(review);
  }

  @Test
  void givenAValidCreationWithPatroniInitialConfig_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getConfigurations()
        .setPatroni(new StackGresClusterPatroni());
    review.getRequest().getObject().getSpec().getConfigurations()
        .getPatroni().setInitialConfig(new StackGresClusterPatroniInitialConfig());
    review.getRequest().getObject().getSpec().getConfigurations()
        .getPatroni().getInitialConfig().put("scope", "test");

    validator.validate(review);
  }

  @Test
  void givenAValidUpdateWithSamePatroniInitialConfig_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getUpdateReview();

    validator.validate(review);
  }

  @Test
  void givenAValidUpdateWithoutPatroniConfig_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getConfigurations()
        .setPatroni(null);
    review.getRequest().getOldObject().getSpec().getConfigurations()
        .setPatroni(null);

    validator.validate(review);
  }

  @Test
  void givenAValidUpdateWithoutPatroniInitialConfig_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getConfigurations()
        .getPatroni().setInitialConfig(null);
    review.getRequest().getOldObject().getSpec().getConfigurations()
        .getPatroni().setInitialConfig(null);

    validator.validate(review);
  }

  @Test
  void givenAnUpdate_shouldFail() {
    final StackGresClusterReview review = getUpdateReview();
    review.getRequest().getObject().getSpec().getConfigurations()
        .getPatroni().getInitialConfig().put("test", true);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        "Cannot update cluster's patroni initial configuration");
  }

  private StackGresClusterReview getCreationReview() {
    return AdmissionReviewFixtures.cluster().loadCreate().get();
  }

  private StackGresClusterReview getUpdateReview() {
    return AdmissionReviewFixtures.cluster().loadPatroniInitialConfigUpdate().get();
  }

}
