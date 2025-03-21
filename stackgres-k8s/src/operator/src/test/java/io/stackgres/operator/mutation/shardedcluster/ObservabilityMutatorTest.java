/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import com.fasterxml.jackson.databind.JsonNode;
import io.stackgres.common.crd.sgcluster.StackGresClusterObservability;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObservabilityMutatorTest {

  private ObservabilityMutator mutator;

  @BeforeEach
  void setUp() throws Exception {
    mutator = new ObservabilityMutator();
  }

  @Test
  void createAlreadyValid_shouldDoNothing() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();

    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void createWithOldFieldsSet_shouldSetObservabilityFields() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();
    review.getRequest().getObject().getSpec().getConfigurations().setObservability(null);
    review.getRequest().getObject().getSpec().getCoordinator().getPods().setDisableMetricsExporter(true);

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getConfigurations()
        .setObservability(new StackGresClusterObservability());
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    review.getRequest().getObject().getSpec().getShards().getPods().setDisableMetricsExporter(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void createWithObservabilityFieldsSet_shouldSetOldFields() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadCreate().get();
    review.getRequest().getObject().getSpec().getConfigurations()
        .setObservability(new StackGresClusterObservability());
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setPrometheusAutobind(true);
    review.getRequest().getObject().getSpec().getCoordinator().getPods().setDisableMetricsExporter(false);

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getCoordinator().getPods().setDisableMetricsExporter(true);
    review.getRequest().getObject().getSpec().getShards().getPods().setDisableMetricsExporter(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateAlreadyValid_shouldChangeShardsOldFields() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadUpdate().get();

    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateWithOldFieldsSet_shouldSetObservabilityFields() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().getCoordinator().getPods().setDisableMetricsExporter(true);

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getConfigurations()
        .setObservability(new StackGresClusterObservability());
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    review.getRequest().getObject().getSpec().getShards().getPods().setDisableMetricsExporter(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateWithObservabilityFieldsChanged_shouldSetOldFields() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setPrometheusAutobind(true);

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getCoordinator().getPods().setDisableMetricsExporter(true);
    review.getRequest().getObject().getSpec().getShards().getPods().setDisableMetricsExporter(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateWithObservabilityFieldsChangedAndOldFieldsChanged_shouldSetOldFieldsAsObservabilityFields() {
    StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setPrometheusAutobind(true);
    review.getRequest().getObject().getSpec().getCoordinator().getPods().setDisableMetricsExporter(null);

    StackGresShardedCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getCoordinator().getPods().setDisableMetricsExporter(true);
    review.getRequest().getObject().getSpec().getShards().getPods().setDisableMetricsExporter(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

}
