/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPostgresSslMutatorTest {

  private StackGresShardedClusterReview review;
  private DefaultPostgresSslMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    mutator = new DefaultPostgresSslMutator();
  }

  @Test
  void clusterWithPostgresSslEnabled_shouldSetNothing() {
    review.getRequest().getObject().getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setEnabled(true);
    StackGresShardedCluster actualCluster = mutate(review);
    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  @Test
  void clusterWithPostgresSslDisabled_shouldSetNothing() {
    review.getRequest().getObject().getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    review.getRequest().getObject().getSpec().getPostgres().getSsl().setEnabled(false);
    StackGresShardedCluster actualCluster = mutate(review);
    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  @Test
  void clusterWithoutPostgreSsl_shouldSetIt() {
    final StackGresShardedCluster actualCluster = mutate(review);

    assertNotEquals(review.getRequest().getObject(), actualCluster);
    assertNotNull(actualCluster.getSpec().getPostgres().getSsl());
    assertTrue(actualCluster.getSpec().getPostgres().getSsl().getEnabled());
  }

  @Test
  void clusterWithoutPostgreSslEnabled_shouldSetIt() {
    final StackGresShardedCluster actualCluster = mutate(review);
    review.getRequest().getObject().getSpec().getPostgres().setSsl(new StackGresClusterSsl());

    assertNotEquals(review.getRequest().getObject(), actualCluster);
    assertTrue(actualCluster.getSpec().getPostgres().getSsl().getEnabled());
  }

  private StackGresShardedCluster mutate(StackGresShardedClusterReview review) {
    return mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
