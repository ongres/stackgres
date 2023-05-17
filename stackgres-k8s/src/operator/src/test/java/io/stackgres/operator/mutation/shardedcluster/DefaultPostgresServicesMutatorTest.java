/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresCoordinatorServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresServices;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterPostgresShardsServices;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultPostgresServicesMutatorTest {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresShardedClusterReview review;
  private DefaultPostgresServicesMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    mutator = new DefaultPostgresServicesMutator();
  }

  @Test
  void clusterWithPostgresService_shouldSetNothing() {
    StackGresPostgresService any = new StackGresPostgresService();
    StackGresPostgresService primary = new StackGresPostgresService();
    StackGresPostgresService primaries = new StackGresPostgresService();
    any.setEnabled(Boolean.TRUE);
    primary.setEnabled(Boolean.TRUE);
    primaries.setEnabled(Boolean.FALSE);
    any.setType("NodeSelector");
    primary.setType("LoadBalancing");
    primaries.setType("ClusterIP");

    setPostgresServices(any, primary, primaries);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterPostgresServices pgServices =
        actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getAny().getEnabled());
    assertEquals("NodeSelector", pgServices.getCoordinator().getAny().getType());
    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getPrimary().getEnabled());
    assertEquals("LoadBalancing", pgServices.getCoordinator().getPrimary().getType());
    assertEquals(Boolean.FALSE, pgServices.getShards().getPrimaries().getEnabled());
    assertEquals("ClusterIP", pgServices.getShards().getPrimaries().getType());
  }

  @Test
  void clusterWithPostgresServiceNoType_shouldSetClusterIP() {
    StackGresPostgresService any = new StackGresPostgresService();
    StackGresPostgresService primary = new StackGresPostgresService();
    StackGresPostgresService primaries = new StackGresPostgresService();
    any.setEnabled(Boolean.TRUE);
    primary.setEnabled(Boolean.TRUE);
    primaries.setEnabled(Boolean.FALSE);

    setPostgresServices(any, primary, primaries);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterPostgresServices pgServices =
        actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getAny().getEnabled());
    assertEquals("ClusterIP", pgServices.getCoordinator().getAny().getType());
    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getPrimary().getEnabled());
    assertEquals("ClusterIP", pgServices.getCoordinator().getPrimary().getType());
    assertEquals(Boolean.FALSE, pgServices.getShards().getPrimaries().getEnabled());
    assertEquals("ClusterIP", pgServices.getShards().getPrimaries().getType());
  }

  @Test
  void clusterWithNoPostgresService_shouldSetValue() {
    StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().getObject().getSpec().setPostgresServices(null);

    StackGresShardedCluster actualCluster = mutate(review);

    StackGresShardedClusterPostgresServices pgServices =
        actualCluster.getSpec().getPostgresServices();
    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getAny().getEnabled());
    assertEquals("ClusterIP", pgServices.getCoordinator().getAny().getType());
    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getPrimary().getEnabled());
    assertEquals("ClusterIP", pgServices.getCoordinator().getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServices.getShards().getPrimaries().getEnabled());
    assertEquals("ClusterIP", pgServices.getShards().getPrimaries().getType());
  }

  @Test
  void clusterWithPostgresServiceNoPrimary_shouldSetValue() {
    StackGresPostgresService primaries = new StackGresPostgresService();
    primaries.setEnabled(Boolean.FALSE);
    primaries.setType("LoadBalancing");

    setPostgresServices(null, null, primaries);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterPostgresServices pgServices =
        actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getAny().getEnabled());
    assertEquals("ClusterIP", pgServices.getCoordinator().getAny().getType());
    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getPrimary().getEnabled());
    assertEquals("ClusterIP", pgServices.getCoordinator().getPrimary().getType());
    assertEquals(Boolean.FALSE, pgServices.getShards().getPrimaries().getEnabled());
    assertEquals("LoadBalancing", pgServices.getShards().getPrimaries().getType());
  }

  @Test
  void clusterWithPostgresServiceNoShards_shouldSetValue() {
    StackGresPostgresService primary = new StackGresPostgresService();
    primary.setEnabled(Boolean.FALSE);
    primary.setType("LoadBalancing");

    setPostgresServices(null, primary, null);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterPostgresServices pgServices =
        actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getAny().getEnabled());
    assertEquals("ClusterIP", pgServices.getCoordinator().getAny().getType());
    assertEquals(Boolean.FALSE, pgServices.getCoordinator().getPrimary().getEnabled());
    assertEquals("LoadBalancing", pgServices.getCoordinator().getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServices.getShards().getPrimaries().getEnabled());
    assertEquals("ClusterIP", pgServices.getShards().getPrimaries().getType());
  }

  @Test
  void clusterWithPostgresServiceNoEnabled_shouldSetValue() {
    StackGresPostgresService primary = new StackGresPostgresService();
    primary.setType("LoadBalancing");

    setPostgresServices(null, primary, null);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterPostgresServices pgServices =
        actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getAny().getEnabled());
    assertEquals("ClusterIP", pgServices.getCoordinator().getAny().getType());
    assertEquals(Boolean.TRUE, pgServices.getCoordinator().getPrimary().getEnabled());
    assertEquals("LoadBalancing", pgServices.getCoordinator().getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServices.getShards().getPrimaries().getEnabled());
    assertEquals("ClusterIP", pgServices.getShards().getPrimaries().getType());
  }

  private void setPostgresServices(
      StackGresPostgresService any,
      StackGresPostgresService primary,
      StackGresPostgresService primaries) {
    StackGresShardedClusterPostgresServices postgresServices =
        new StackGresShardedClusterPostgresServices();
    postgresServices.setCoordinator(new StackGresShardedClusterPostgresCoordinatorServices());
    postgresServices.setShards(new StackGresShardedClusterPostgresShardsServices());
    postgresServices.getCoordinator().setAny(any);
    postgresServices.getCoordinator().setPrimary(primary);
    postgresServices.getShards().setPrimaries(primaries);

    review.getRequest().getObject().getSpec().setPostgresServices(postgresServices);
  }

  private StackGresShardedCluster mutate(StackGresShardedClusterReview review) {
    return mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
