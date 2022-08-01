/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicationGroup;
import io.stackgres.common.crd.sgcluster.StackGresMainReplicationRole;
import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.common.crd.sgcluster.StackGresReplicationRole;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

class DefaultReplicationMutatorTest {

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresClusterReview review;
  private DefaultReplicationMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();

    mutator = new DefaultReplicationMutator();
    mutator.setObjectMapper(JSON_MAPPER);
    mutator.init();
  }

  @Test
  void clusterWithDefaultReplication_shouldSetNothing() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setMode(StackGresReplicationMode.ASYNC.toString());
    replication.setRole(StackGresMainReplicationRole.HA_READ.toString());

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA_READ.toString(), actualReplication.getRole());
    assertNull(actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithoutMode_shouldSetAsyncMode() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setRole(StackGresMainReplicationRole.HA_READ.toString());

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA_READ.toString(), actualReplication.getRole());
    assertNull(actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithoutRole_shouldSetHaReadRole() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setMode(StackGresReplicationMode.ASYNC.toString());

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA_READ.toString(), actualReplication.getRole());
    assertNull(actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithRole_shouldSetNothing() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setMode(StackGresReplicationMode.ASYNC.toString());
    replication.setRole(StackGresMainReplicationRole.HA.toString());

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA.toString(), actualReplication.getRole());
    assertNull(actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithSyncMode_shouldSetSyncNodeCount() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setMode(StackGresReplicationMode.SYNC.toString());
    replication.setRole(StackGresMainReplicationRole.HA_READ.toString());

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.SYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA_READ.toString(), actualReplication.getRole());
    assertEquals(1, actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithStrictSyncMode_shouldSetSyncNodeCount() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setMode(StackGresReplicationMode.STRICT_SYNC.toString());
    replication.setRole(StackGresMainReplicationRole.HA_READ.toString());

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.STRICT_SYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA_READ.toString(), actualReplication.getRole());
    assertEquals(1, actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithSyncModeAndSyncNodeCount_shouldSetNothing() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setMode(StackGresReplicationMode.SYNC.toString());
    replication.setRole(StackGresMainReplicationRole.HA_READ.toString());
    replication.setSyncInstances(2);

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.SYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA_READ.toString(), actualReplication.getRole());
    assertEquals(2, actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithStrictSyncModeAndSyncNodeCount_shouldSetNothing() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setMode(StackGresReplicationMode.STRICT_SYNC.toString());
    replication.setRole(StackGresMainReplicationRole.HA_READ.toString());
    replication.setSyncInstances(2);

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.STRICT_SYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA_READ.toString(), actualReplication.getRole());
    assertEquals(2, actualReplication.getSyncInstances());
    assertNull(actualReplication.getGroups());
  }

  @Test
  void clusterWithGroupWithoutName_shouldSetGroupName() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setMode(StackGresReplicationMode.ASYNC.toString());
    replication.setRole(StackGresMainReplicationRole.HA_READ.toString());
    StackGresClusterReplicationGroup replicationGroup = new StackGresClusterReplicationGroup();
    replicationGroup.setRole(StackGresReplicationRole.READONLY.toString());
    replicationGroup.setInstances(1);
    replication.setGroups(ImmutableList.of(replicationGroup));

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA_READ.toString(), actualReplication.getRole());
    assertNull(actualReplication.getSyncInstances());
    assertNotNull(actualReplication.getGroups());
    assertEquals(1, actualReplication.getGroups().size());
    assertEquals("group-1", actualReplication.getGroups().get(0).getName());
    assertEquals(StackGresReplicationRole.READONLY.toString(),
        actualReplication.getGroups().get(0).getRole());
    assertEquals(1, actualReplication.getGroups().get(0).getInstances());
  }

  @Test
  void clusterWithGroupWithoutRole_shouldSetHaReadRole() {
    StackGresClusterReplication replication = new StackGresClusterReplication();
    replication.setMode(StackGresReplicationMode.ASYNC.toString());
    replication.setRole(StackGresMainReplicationRole.HA_READ.toString());
    StackGresClusterReplicationGroup replicationGroup = new StackGresClusterReplicationGroup();
    replicationGroup.setName("test-1");
    replicationGroup.setInstances(1);
    replication.setGroups(ImmutableList.of(replicationGroup));

    review.getRequest().getObject().getSpec().setReplication(replication);
    StackGresCluster actualCluster = mutate(review);
    StackGresClusterReplication actualReplication =
        actualCluster.getSpec().getReplication();

    assertEquals(StackGresReplicationMode.ASYNC.toString(), actualReplication.getMode());
    assertEquals(StackGresMainReplicationRole.HA_READ.toString(), actualReplication.getRole());
    assertNull(actualReplication.getSyncInstances());
    assertNotNull(actualReplication.getGroups());
    assertEquals(1, actualReplication.getGroups().size());
    assertEquals("test-1", actualReplication.getGroups().get(0).getName());
    assertEquals(StackGresReplicationRole.HA_READ.toString(),
        actualReplication.getGroups().get(0).getRole());
    assertEquals(1, actualReplication.getGroups().get(0).getInstances());
  }

  private StackGresCluster mutate(StackGresClusterReview review) {
    try {
      List<JsonPatchOperation> operations = mutator.mutate(review);
      JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());
      JsonNode newConfig = new JsonPatch(operations).apply(crJson);
      return JSON_MAPPER.treeToValue(newConfig, StackGresCluster.class);
    } catch (JsonPatchException | JsonProcessingException | IllegalArgumentException e) {
      throw new AssertionFailedError(e.getMessage(), e);
    }
  }
}
