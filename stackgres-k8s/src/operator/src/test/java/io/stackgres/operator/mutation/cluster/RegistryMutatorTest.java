/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterRegistry;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistryMutatorTest {

  private RegistryMutator mutator;

  @BeforeEach
  void setUp() throws Exception {
    mutator = new RegistryMutator();
  }

  @Test
  void createWithoutRegistry_shouldEnableIt() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreate().get();
    review.getRequest().getObject().getSpec().getConfigurations().setRegistry(null);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(Boolean.TRUE,
        result.getSpec().getConfigurations().getRegistry().getEnabled());
  }

  @Test
  void createWithRegistryDisabled_shouldKeepIt() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreate().get();
    StackGresClusterRegistry registry = new StackGresClusterRegistry();
    registry.setEnabled(false);
    review.getRequest().getObject().getSpec().getConfigurations().setRegistry(registry);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(Boolean.FALSE,
        result.getSpec().getConfigurations().getRegistry().getEnabled());
  }

  @Test
  void updateWithoutRegistry_shouldDisableIt() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().getConfigurations().setRegistry(null);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(Boolean.FALSE,
        result.getSpec().getConfigurations().getRegistry().getEnabled());
  }

  @Test
  void updateWithRegistryEnabled_shouldKeepIt() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdate().get();
    StackGresClusterRegistry registry = new StackGresClusterRegistry();
    registry.setEnabled(true);
    registry.setUrl("https://sgcr.dev");
    review.getRequest().getObject().getSpec().getConfigurations().setRegistry(registry);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(registry, result.getSpec().getConfigurations().getRegistry());
  }

}
