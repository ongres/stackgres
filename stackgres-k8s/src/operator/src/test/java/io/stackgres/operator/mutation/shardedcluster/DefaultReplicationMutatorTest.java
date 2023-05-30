/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterReplication;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultReplicationMutatorTest {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresShardedClusterReview review;
  private DefaultReplicationMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    mutator = new DefaultReplicationMutator();
  }

  @Test
  void clusterWithDefaultReplication_shouldSetNothing() {
    StackGresShardedClusterReplication replication = new StackGresShardedClusterReplication();
    replication.setMode(StackGresReplicationMode.ASYNC.toString());

    review.getRequest().getObject().getSpec()
        .setReplication(replication);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertNull(actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithoutMode_shouldSetAsyncMode() {
    StackGresShardedClusterReplication replication = new StackGresShardedClusterReplication();

    review.getRequest().getObject().getSpec()
        .setReplication(replication);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertNull(actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithoutRole_shouldSetHaReadRole() {
    StackGresShardedClusterReplication replication = new StackGresShardedClusterReplication();
    replication.setMode(StackGresReplicationMode.ASYNC.toString());

    review.getRequest().getObject().getSpec()
        .setReplication(replication);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertNull(actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithRole_shouldSetNothing() {
    StackGresShardedClusterReplication replication = new StackGresShardedClusterReplication();
    replication.setMode(StackGresReplicationMode.ASYNC.toString());

    review.getRequest().getObject().getSpec()
        .setReplication(replication);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertNull(actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithSyncMode_shouldSetSyncNodeCount() {
    StackGresShardedClusterReplication replication = new StackGresShardedClusterReplication();
    replication.setMode(StackGresReplicationMode.SYNC.toString());

    review.getRequest().getObject().getSpec()
        .setReplication(replication);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.SYNC.toString(), actualReplication.getMode());
    assertNull(actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithSyncModeAndSyncNodeCount_shouldSetNothing() {
    StackGresShardedClusterReplication replication = new StackGresShardedClusterReplication();
    replication.setMode(StackGresReplicationMode.SYNC.toString());
    replication.setSyncInstances(2);

    review.getRequest().getObject().getSpec()
        .setReplication(replication);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.SYNC.toString(), actualReplication.getMode());
    assertEquals(2, actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithStrictSyncModeAndSyncNodeCount_shouldSetNothing() {
    StackGresShardedClusterReplication replication = new StackGresShardedClusterReplication();
    replication.setMode(StackGresReplicationMode.STRICT_SYNC.toString());
    replication.setSyncInstances(2);

    review.getRequest().getObject().getSpec()
        .setReplication(replication);
    StackGresShardedCluster actualCluster = mutate(review);
    StackGresShardedClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.STRICT_SYNC.toString(), actualReplication.getMode());
    assertEquals(2, actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  private StackGresShardedCluster mutate(StackGresShardedClusterReview review) {
    return mutator.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
