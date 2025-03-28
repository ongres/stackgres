/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterObservability;
import io.stackgres.operator.common.StackGresClusterReview;
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
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreate().get();

    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void createWithOldFieldsSet_shouldSetObservabilityFields() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreate().get();
    review.getRequest().getObject().getSpec().getConfigurations().setObservability(null);
    review.getRequest().getObject().getSpec().getPods().setDisableMetricsExporter(true);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getConfigurations()
        .setObservability(new StackGresClusterObservability());
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void createWithObservabilityFieldsSet_shouldSetOldFields() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadCreate().get();
    review.getRequest().getObject().getSpec().getConfigurations()
        .setObservability(new StackGresClusterObservability());
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setPrometheusAutobind(true);
    review.getRequest().getObject().getSpec().getPods().setDisableMetricsExporter(false);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getPods().setDisableMetricsExporter(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateAlreadyValid_shouldDoNothing() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdate().get();

    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateWithOldFieldsSet_shouldSetObservabilityFields() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().getPods().setDisableMetricsExporter(true);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getConfigurations()
        .setObservability(new StackGresClusterObservability());
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateWithObservabilityFieldsChanged_shouldSetOldFields() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setPrometheusAutobind(true);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getPods().setDisableMetricsExporter(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

  @Test
  void updateWithObservabilityFieldsChangedAndOldFieldsChanged_shouldSetOldFieldsAsObservabilityFields() {
    StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadUpdate().get();
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setDisableMetrics(true);
    review.getRequest().getObject().getSpec().getConfigurations().getObservability().setPrometheusAutobind(true);
    review.getRequest().getObject().getSpec().getPods().setDisableMetricsExporter(null);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    review.getRequest().getObject().getSpec().getPods().setDisableMetricsExporter(true);
    JsonNode expectedCluster = JsonUtil.toJson(review.getRequest().getObject());

    JsonUtil.assertJsonEquals(
        expectedCluster,
        JsonUtil.toJson(result));
  }

}
