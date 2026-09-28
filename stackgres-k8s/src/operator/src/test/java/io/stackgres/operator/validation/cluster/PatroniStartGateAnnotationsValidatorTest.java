/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatroniStartGateAnnotationsValidatorTest {

  private PatroniStartGateAnnotationsValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PatroniStartGateAnnotationsValidator();
  }

  @Test
  void givenACreationWithoutStartGateAnnotations_shouldPass() throws ValidationFailed {
    validator.validate(getCreationReview(null));
  }

  @Test
  void givenACreationWithValidStartGateAnnotations_shouldPass() throws ValidationFailed {
    validator.validate(getCreationReview(Map.of(
        "stackgres.io/citus-group-registered", "1025",
        "ready", "true")));
  }

  @Test
  void givenACreationWithAnInvalidStartGateAnnotationKey_shouldFail() {
    final StackGresClusterReview review = getCreationReview(Map.of("in valid", "true"));

    assertThrows(ValidationFailed.class, () -> validator.validate(review));
  }

  @Test
  void givenACreationWithAnyStartGateAnnotationValue_shouldPass() throws ValidationFailed {
    validator.validate(getCreationReview(Map.of("valid", "any value, even with spaces")));
  }

  private StackGresClusterReview getCreationReview(Map<String, String> startGateAnnotations) {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getConfigurations()
        .setPatroni(new StackGresClusterPatroni());
    review.getRequest().getObject().getSpec().getConfigurations()
        .getPatroni().setStartGateAnnotations(startGateAnnotations);
    return review;
  }

}
