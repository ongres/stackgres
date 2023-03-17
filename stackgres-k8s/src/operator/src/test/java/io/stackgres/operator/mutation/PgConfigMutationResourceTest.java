/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationResource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PgConfigMutationResourceTest
    extends MutationResourceTest<StackGresPostgresConfig, PgConfigReview> {

  @Override
  protected MutationResource<StackGresPostgresConfig, PgConfigReview> getResource() {
    return new PgConfigMutationResource(pipeline);
  }

  @Override
  protected PgConfigReview getReview() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

}
