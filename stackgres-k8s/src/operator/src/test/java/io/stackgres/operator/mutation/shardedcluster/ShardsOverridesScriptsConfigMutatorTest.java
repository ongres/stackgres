/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardsOverridesScriptsConfigMutatorTest {

  protected static final ObjectMapper JSON_MAPPER = new ObjectMapper();

  private ShardsOverridesScriptsConfigMutator mutator;

  @BeforeEach
  void setUp() throws Exception {
    mutator = new ShardsOverridesScriptsConfigMutator();
  }

  @Test
  void createScriptAlreadyValid_shouldDoNothing() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreateWithManagedSql().get();

    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void createClusterWithouIds_shouldAddThem() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreateWithManagedSql().get();
    final JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    review.getRequest().getObject().getSpec().getShards().getOverrides().get(0)
        .getManagedSql().getScripts().stream()
        .forEach(scriptEntry -> scriptEntry.setId(null));

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateClusterWithWithoutModification_shouldDoNothing() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadUpdateWithManagedSql().get();

    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateClusterAddingAnEntry_shouldSetId() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadUpdateWithManagedSql().get();

    review.getRequest().getObject().getSpec().getShards().getOverrides().get(0)
        .getManagedSql().getScripts().add(1, new StackGresClusterManagedScriptEntry());

    StackGresShardedCluster expected = JsonUtil.copy(review.getRequest().getObject());
    expected.getSpec().getShards().getOverrides().get(0)
        .getManagedSql().getScripts().get(1).setId(4);
    JsonNode expectedCluster = JsonUtil.toJson(expected);

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

}
