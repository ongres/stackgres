/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresPostgresConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PgConfigNormalizeValuesMutatorTest {

  private PgConfigMutator mutator = new PgConfigNormalizeValuesMutator();

  private StackGresPostgresConfigReview getDefaultReview() {
    return AdmissionReviewFixtures.postgresConfig().loadCreate().get();
  }

  private StackGresPostgresConfigReview getEmptyReview() {
    StackGresPostgresConfigReview review = getDefaultReview();
    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());
    return review;
  }

  @Test
  void givenEmptyConfig_shouldNotReturnValues() {
    StackGresPostgresConfigReview review = getEmptyReview();

    StackGresPostgresConfig result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        JsonUtil.toJson(review.getRequest().getObject()),
        JsonUtil.toJson(result));
  }

  @Test
  void givenAConfig_shouldReturnNormalizedValues() {
    StackGresPostgresConfigReview review = getDefaultReview();
    Map<String, String> postgresqlConf =
        review.getRequest().getObject().getSpec().getPostgresqlConf();
    postgresqlConf.put("shared_buffers", "30822MB");
    postgresqlConf.put("max_worker_processes", "16");
    postgresqlConf.put("password_encryption", "on");
    postgresqlConf.put("DateStyle", "ISO");
    postgresqlConf.put("work_mem", "16794kB");
    final JsonNode expectedPostgresConfig = JsonUtil.toJson(
        review.getRequest().getObject());
    postgresqlConf.put("shared_buffers", "30.1GB");
    postgresqlConf.put("max_worker_processes", "16");
    postgresqlConf.put("invalid_param", "true");
    postgresqlConf.put("password_encryption", "on");
    postgresqlConf.put("work_mem", "16.4MB");
    postgresqlConf.put("datestyle", "ISO");

    StackGresPostgresConfig result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedPostgresConfig,
        JsonUtil.toJson(result));
  }

  @ParameterizedTest
  @CsvSource(value = {"2147483647,2147483647MB", "62.37GB,63867MB", "0,0MB",
      "0.99,1MB", "567.79TB,581417GB", "2147483647B,2GB"})
  void givenMemoryConfig_shouldReturnNormalizedValues(String value, String expected) {
    StackGresPostgresConfigReview review = getEmptyReview();
    review.getRequest().getObject().getSpec().setPostgresVersion("14.2");
    review.getRequest().getObject().getSpec().getPostgresqlConf()
        .put("min_dynamic_shared_memory", expected);
    final JsonNode expectedPostgresConfig = JsonUtil.toJson(
        review.getRequest().getObject());
    review.getRequest().getObject().getSpec().getPostgresqlConf()
        .put("min_dynamic_shared_memory", value);

    StackGresPostgresConfig result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    JsonUtil.assertJsonEquals(
        expectedPostgresConfig,
        JsonUtil.toJson(result));
  }

}
