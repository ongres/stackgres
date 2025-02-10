/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgConfigMutationResourceTest
    extends MutationResourceTest<StackGresPostgresConfig, StackGresPostgresConfigReview> {

  @Override
  protected AbstractMutationResource<StackGresPostgresConfig, StackGresPostgresConfigReview> getResource() {
    return new PgConfigMutationResource(JsonUtil.jsonMapper(), pipeline);
  }

  @Override
  protected StackGresPostgresConfigReview getReview() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

}
