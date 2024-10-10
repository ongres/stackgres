/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.Map;

import io.stackgres.common.StackGresKeys;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorkers;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShardsToWorkersMutatorTest {

  private ShardsToWorkersMutator mutator;

  @BeforeEach
  void setUp() {
    mutator = new ShardsToWorkersMutator();
  }

  private StackGresShardedClusterReview updateReviewWithDeprecatedShards() {
    var review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().setOperation(Operation.UPDATE);

    var metadata = review.getRequest().getObject().getMetadata();
    Map<String, String> annotations = metadata.getAnnotations();
    if (annotations == null) {
      annotations = new HashMap<>();
      metadata.setAnnotations(annotations);
    }
    annotations.put(StackGresKeys.VERSION_KEY, StackGresVersion.V_1_18.getVersion());

    var spec = review.getRequest().getObject().getSpec();
    StackGresShardedClusterWorkers deprecatedShards = spec.getWorkers();
    spec.setWorkers(null);
    spec.setShards(deprecatedShards);

    return review;
  }

  @Test
  void onUpdate_whenShardsSetAndWorkersNull_shouldCopyShardsToWorkers() {
    StackGresShardedClusterReview review = updateReviewWithDeprecatedShards();
    StackGresShardedClusterWorkers shards = review.getRequest().getObject().getSpec().getShards();

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertNotNull(result.getSpec().getWorkers());
    assertEquals(shards.getClusters(), result.getSpec().getWorkers().getClusters());
    assertEquals(
        shards.getInstancesPerCluster(),
        result.getSpec().getWorkers().getInstancesPerCluster());
  }

  @Test
  void onUpdate_whenShardsSetAndWorkersNull_shouldSetClusterNameTemplateToShardPrefix() {
    StackGresShardedClusterReview review = updateReviewWithDeprecatedShards();
    String name = review.getRequest().getObject().getMetadata().getName();

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(name + "-shard", result.getSpec().getWorkers().getClusterNameTemplate());
  }

  @Test
  void onUpdate_whenShardsNullAndWorkersSet_shouldNotMutate() {
    var review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().setOperation(Operation.UPDATE);
    var originalWorkers = review.getRequest().getObject().getSpec().getWorkers();

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertNull(result.getSpec().getShards());
    assertEquals(originalWorkers.getClusters(), result.getSpec().getWorkers().getClusters());
    assertNull(result.getSpec().getWorkers().getClusterNameTemplate());
  }

  @Test
  void onUpdate_whenBothShardsAndWorkersSet_shouldNotOverwriteWorkers() {
    var review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().setOperation(Operation.UPDATE);
    var workers = review.getRequest().getObject().getSpec().getWorkers();
    review.getRequest().getObject().getSpec().setShards(new StackGresShardedClusterWorkers());

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(workers.getClusters(), result.getSpec().getWorkers().getClusters());
    assertNull(result.getSpec().getWorkers().getClusterNameTemplate());
  }

  @Test
  void onCreate_shouldNotMutate() {
    StackGresShardedClusterReview review = updateReviewWithDeprecatedShards();
    review.getRequest().setOperation(Operation.CREATE);

    StackGresShardedCluster resource = JsonUtil.copy(review.getRequest().getObject());
    StackGresShardedCluster result = mutator.mutate(review, resource);

    assertSame(resource, result);
    assertNull(result.getSpec().getWorkers());
    assertNotNull(result.getSpec().getShards());
  }

  @Test
  void onDelete_shouldNotMutate() {
    StackGresShardedClusterReview review = updateReviewWithDeprecatedShards();
    review.getRequest().setOperation(Operation.DELETE);

    StackGresShardedCluster resource = JsonUtil.copy(review.getRequest().getObject());
    StackGresShardedCluster result = mutator.mutate(review, resource);

    assertSame(resource, result);
    assertNull(result.getSpec().getWorkers());
  }

}
