/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultPostgresServicesMutatorTest {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresClusterReview review;
  private DefaultPostgresServicesMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();

    mutator = new DefaultPostgresServicesMutator();
  }

  @Test
  void clusterWithPostgresService_shouldSetNothing() {
    StackGresPostgresService primary = new StackGresPostgresService();
    StackGresPostgresService replica = new StackGresPostgresService();
    primary.setEnabled(Boolean.TRUE);
    replica.setEnabled(Boolean.FALSE);
    primary.setType("LoadBalancing");
    replica.setType("ClusterIP");

    setPostgresServices(primary, replica);
    StackGresCluster actualCluster = mutate(review);
    StackGresPostgresServices pgServices = actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.TRUE, pgServices.getPrimary().getEnabled());
    assertEquals("LoadBalancing", pgServices.getPrimary().getType());
    assertEquals(Boolean.FALSE, pgServices.getReplicas().getEnabled());
    assertEquals("ClusterIP", pgServices.getReplicas().getType());
  }

  @Test
  void clusterWithPostgresServiceNoType_shouldSetClusterIP() {
    StackGresPostgresService primary = new StackGresPostgresService();
    StackGresPostgresService replica = new StackGresPostgresService();
    primary.setEnabled(Boolean.TRUE);
    replica.setEnabled(Boolean.FALSE);

    setPostgresServices(primary, replica);
    StackGresCluster actualCluster = mutate(review);
    StackGresPostgresServices pgServices = actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.TRUE, pgServices.getPrimary().getEnabled());
    assertEquals("ClusterIP", pgServices.getPrimary().getType());
    assertEquals(Boolean.FALSE, pgServices.getReplicas().getEnabled());
    assertEquals("ClusterIP", pgServices.getReplicas().getType());
  }

  @Test
  void clusterWithNoPostgresService_shouldSetValue() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec().setPostgresServices(null);

    StackGresCluster actualCluster = mutate(review);

    StackGresPostgresServices pgServices = actualCluster.getSpec().getPostgresServices();
    assertEquals(Boolean.TRUE, pgServices.getPrimary().getEnabled());
    assertEquals("ClusterIP", pgServices.getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServices.getReplicas().getEnabled());
    assertEquals("ClusterIP", pgServices.getReplicas().getType());
  }

  @Test
  void clusterWithPostgresServiceNoPrimary_shouldSetValue() {
    StackGresPostgresService replica = new StackGresPostgresService();
    replica.setEnabled(Boolean.FALSE);
    replica.setType("LoadBalancing");

    setPostgresServices(null, replica);
    StackGresCluster actualCluster = mutate(review);
    StackGresPostgresServices pgServices = actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.TRUE, pgServices.getPrimary().getEnabled());
    assertEquals("ClusterIP", pgServices.getPrimary().getType());
    assertEquals(Boolean.FALSE, pgServices.getReplicas().getEnabled());
    assertEquals("LoadBalancing", pgServices.getReplicas().getType());
  }

  @Test
  void clusterWithPostgresServiceNoReplicas_shouldSetValue() {
    StackGresPostgresService primary = new StackGresPostgresService();
    primary.setEnabled(Boolean.FALSE);
    primary.setType("LoadBalancing");

    setPostgresServices(primary, null);
    StackGresCluster actualCluster = mutate(review);
    StackGresPostgresServices pgServices = actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.FALSE, pgServices.getPrimary().getEnabled());
    assertEquals("LoadBalancing", pgServices.getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServices.getReplicas().getEnabled());
    assertEquals("ClusterIP", pgServices.getReplicas().getType());
  }

  @Test
  void clusterWithPostgresServiceNoEnabled_shouldSetValue() {
    StackGresPostgresService primary = new StackGresPostgresService();
    primary.setType("LoadBalancing");

    setPostgresServices(primary, null);
    StackGresCluster actualCluster = mutate(review);
    StackGresPostgresServices pgServices = actualCluster.getSpec().getPostgresServices();

    assertEquals(Boolean.TRUE, pgServices.getPrimary().getEnabled());
    assertEquals("LoadBalancing", pgServices.getPrimary().getType());
    assertEquals(Boolean.TRUE, pgServices.getReplicas().getEnabled());
    assertEquals("ClusterIP", pgServices.getReplicas().getType());
  }

  private void setPostgresServices(StackGresPostgresService primary,
      StackGresPostgresService replica) {
    StackGresPostgresServices postgresServices = new StackGresPostgresServices();
    postgresServices.setPrimary(primary);
    postgresServices.setReplicas(replica);

    review.getRequest().getObject().getSpec().setPostgresServices(postgresServices);
  }

  private StackGresCluster mutate(StackGresClusterReview review) {
    return mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
