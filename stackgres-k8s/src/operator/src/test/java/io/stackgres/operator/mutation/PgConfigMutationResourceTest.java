/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PgConfigMutationResourceTest extends MutationResourceTest<PgConfigReview> {

  @BeforeEach
  void setUp() {
    final PgConfigMutationResource resource = new PgConfigMutationResource();
    resource.setPipeline(pipeline);
    this.resource = resource;

    review = AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  @Override
  @Test
  void givenAnValidAdmissionReview_itShouldReturnAnyPath() {
    super.givenAnValidAdmissionReview_itShouldReturnAnyPath();
  }

  @Override
  @Test
  void givenAnInvalidAdmissionReview_itShouldReturnABase64EncodedPath() {
    super.givenAnInvalidAdmissionReview_itShouldReturnABase64EncodedPath();
  }
}
