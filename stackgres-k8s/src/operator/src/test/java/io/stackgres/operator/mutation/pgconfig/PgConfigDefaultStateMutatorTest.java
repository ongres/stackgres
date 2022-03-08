/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.mutation.DefaultStateMutator;
import io.stackgres.operator.mutation.DefaultStateMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PgConfigDefaultStateMutatorTest
    extends DefaultStateMutatorTest<StackGresPostgresConfig, PgConfigReview> {

  @Override
  protected DefaultStateMutator<StackGresPostgresConfig, PgConfigReview> getMutatorInstance() {
    return new PgConfigDefaultStateMutator();
  }

  @Override
  protected PgConfigReview getEmptyReview() {
    PgConfigReview review = AdmissionReviewFixtures.postgresConfig().loadCreate().get();
    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());
    return review;
  }

  @Override
  protected PgConfigReview getDefaultReview() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  @Override
  protected StackGresPostgresConfig getDefaultResource() {
    return Fixtures.postgresConfig().loadDefault().get();
  }

  @Override
  protected JsonNode getConfJson(JsonNode crJson) {
    return crJson.get("status").get("defaultParameters");
  }

  @Override
  protected int getMissingParentsCount() {
    return 1;
  }

  @Override
  protected Map<String, String> getConfigParameters(StackGresPostgresConfig resource) {
    return resource.getStatus().getDefaultParameters();
  }

}
