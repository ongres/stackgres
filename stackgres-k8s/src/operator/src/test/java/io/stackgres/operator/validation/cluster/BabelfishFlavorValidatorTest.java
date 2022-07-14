/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import com.google.common.collect.Lists;
import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresFeatureGates;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BabelfishFlavorValidatorTest {

  private static final String BABELFISH_VERSION =
      StackGresComponent.BABELFISH.getLatest().streamOrderedVersions().findFirst().get();

  private BabelfishFlavorValidator validator;

  @BeforeEach
  void setUp() {
    validator = new BabelfishFlavorValidator();
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(BABELFISH_VERSION);
    review.getRequest().getObject().getSpec().getPostgres().setFlavor(
        StackGresPostgresFlavor.BABELFISH.toString());
    review.getRequest().getObject().getSpec().setNonProductionOptions(
        new StackGresClusterNonProduction()
    );
    review.getRequest().getObject().getSpec().getNonProductionOptions()
        .setEnabledFeatureGates(Lists.newArrayList(
            StackGresFeatureGates.BABELFISH_FLAVOR.toString()));
    validator.validate(review);
  }

  @Test
  void givenACreationWithMissingBabelfishFlavorGate_shouldFail() {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(BABELFISH_VERSION);
    review.getRequest().getObject().getSpec().getPostgres().setFlavor(
        StackGresPostgresFlavor.BABELFISH.toString());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "To enable \"babelfish\" flavor you must add \"babelfish-flavor\" feature gate under"
            + " \".spec.nonProductionOptions.enabledFeatureGates\"",
            ".spec.nonProductionOptions.enabledFeatureGates");
  }

  @Test
  void givenACreationWithMoreThanOneInstances_shouldFail() {
    final StackGresClusterReview review = getCreationReview();
    review.getRequest().getObject().getSpec().setInstances(2);
    review.getRequest().getObject().getSpec().getPostgres().setVersion(BABELFISH_VERSION);
    review.getRequest().getObject().getSpec().getPostgres().setFlavor(
        StackGresPostgresFlavor.BABELFISH.toString());
    review.getRequest().getObject().getSpec().setNonProductionOptions(
        new StackGresClusterNonProduction()
    );
    review.getRequest().getObject().getSpec().getNonProductionOptions()
        .setEnabledFeatureGates(Lists.newArrayList(
            StackGresFeatureGates.BABELFISH_FLAVOR.toString()));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Currently \"babelfish\" flavor only support 1 instance."
            + " Please set \".spec.instances\" to 1",
            ".spec.instances");
  }

  private StackGresClusterReview getCreationReview() {
    return JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json",
            StackGresClusterReview.class);
  }

}
