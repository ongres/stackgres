/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.pgconfig;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.MoveOperation;
import com.github.fge.jsonpatch.RemoveOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.operator.common.PgConfigReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PgConfigNormalizeValuesMutatorTest {

  private PgConfigMutator mutator = new PgConfigNormalizeValuesMutator();

  private PgConfigReview getDefaultReview() {
    return JsonUtil
        .readFromJson("pgconfig_allow_request/valid_pgconfig.json", PgConfigReview.class);
  }

  private PgConfigReview getEmptyReview() {
    PgConfigReview review = getDefaultReview();
    review.getRequest().getObject().getSpec().setPostgresqlConf(new HashMap<>());
    return review;
  }

  @Test
  void givenEmptyConfig_shouldNotReturnValues() {
    PgConfigReview review = getEmptyReview();
    List<JsonPatchOperation> operators = mutator.mutate(review);

    assertThat(operators).isNotNull();
    assertThat(operators).hasSize(0);
  }

  @Test
  void givenAConfig_shouldReturnNormalizedValues() {
    PgConfigReview review = getDefaultReview();
    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    StackGresPostgresConfigSpec spec = pgConfig.getSpec();
    Map<String, String> postgresqlConf = spec.getPostgresqlConf();
    postgresqlConf.put("shared_buffers", "30.1GB");
    postgresqlConf.put("max_worker_processes", "16");
    postgresqlConf.put("invalid_param", "true");
    postgresqlConf.put("password_encryption", "on");
    postgresqlConf.put("work_mem", "16.4MB");
    postgresqlConf.put("datestyle", "ISO");

    List<JsonPatchOperation> operators = mutator.mutate(review);

    JsonPatchOperation sharedBuffersPatch =
        new ReplaceOperation(PgConfigMutator.PG_CONFIG_POINTER.append("shared_buffers"),
            TextNode.valueOf("30822MB"));
    JsonPatchOperation workMemPatch =
        new ReplaceOperation(PgConfigMutator.PG_CONFIG_POINTER.append("work_mem"),
            TextNode.valueOf("16794kB"));
    JsonPatchOperation datestylePatch =
        new MoveOperation(PgConfigMutator.PG_CONFIG_POINTER.append("datestyle"),
            PgConfigMutator.PG_CONFIG_POINTER.append("DateStyle"));
    JsonPatchOperation removePatch =
        new RemoveOperation(PgConfigMutator.PG_CONFIG_POINTER.append("invalid_param"));

    assertThat(operators).isNotNull();
    assertThat(operators).hasSize(4);
    assertThat(operators).containsNoDuplicates();

    operators.forEach(c -> {
      assertThat(c.toString())
          .isAnyOf(sharedBuffersPatch.toString(), workMemPatch.toString(),
              datestylePatch.toString(), removePatch.toString());
    });
  }

  @ParameterizedTest
  @CsvSource(value = {"2147483647,2147483647MB", "62.37GB,63867MB", "0,0MB",
      "0.99,1MB", "567.79TB,581417GB", "2147483647B,2GB"})
  void givenMemoryConfig_shouldReturnNormalizedValues(String value, String expected) {
    PgConfigReview review = getEmptyReview();
    review.getRequest().getObject().getSpec().setPostgresVersion("14.2.9");
    StackGresPostgresConfig pgConfig = review.getRequest().getObject();
    pgConfig.getSpec().getPostgresqlConf().put("min_dynamic_shared_memory", value);

    List<JsonPatchOperation> operators = mutator.mutate(review);

    JsonPatchOperation patch =
        new ReplaceOperation(PgConfigMutator.PG_CONFIG_POINTER.append("min_dynamic_shared_memory"),
            TextNode.valueOf(expected));

    assertThat(operators).isNotNull();
    assertThat(operators).hasSize(1);
    assertThat(operators.get(0).toString()).isEqualTo(patch.toString());
  }

}
