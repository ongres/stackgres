/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.HashMap;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultClusterPostgresConfigFactory;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.DefaultValuesMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgConfigDefaultValuesMutatorTest
    extends DefaultValuesMutatorTest<StackGresPostgresConfig, StackGresPostgresConfigReview, StackGresCluster> {

  @Override
  protected PgConfigDefaultValuesMutator getMutatorInstance(
      DefaultCustomResourceFactory<StackGresPostgresConfig, StackGresCluster> factory,
      JsonMapper jsonMapper) {
    return new PgConfigDefaultValuesMutator(factory, jsonMapper);
  }

  @Override
  protected DefaultCustomResourceFactory<StackGresPostgresConfig, StackGresCluster> createFactory() {
    return new DefaultClusterPostgresConfigFactory();
  }

  @Override
  protected StackGresPostgresConfigReview getEmptyReview() {
    StackGresPostgresConfigReview review = AdmissionReviewFixtures.postgresConfig().loadCreate().get();
    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());
    return review;
  }

  @Override
  protected StackGresPostgresConfigReview getDefaultReview() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  @Override
  protected StackGresPostgresConfig getDefaultResource() {
    return new PgConfigNormalizeValuesMutator().mutate(
        getDefaultReview(),
        factory.buildResource(
            Fixtures.cluster().loadDefault().getBuilder()
            .editSpec()
            .editPostgres()
            .withVersion(getDefaultReview().getRequest().getObject().getSpec().getPostgresVersion())
            .endPostgres()
            .endSpec()
            .build()));
  }

}
