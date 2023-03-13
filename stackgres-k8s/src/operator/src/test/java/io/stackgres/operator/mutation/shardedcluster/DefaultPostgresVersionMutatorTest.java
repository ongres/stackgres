/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.StackGresComponent;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPostgresVersionMutatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresShardedClusterReview review;

  private DefaultPostgresVersionMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    mutator = new DefaultPostgresVersionMutator();
    mutator.setObjectMapper(JSON_MAPPER);
    mutator.init();
  }

  @Test
  void clusterWithFinalPostgresVersion_shouldNotDoAnything() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());
  }

  @Test
  void clusteWithNoPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newConfig = jp.apply(crJson);

    String actualPostgresVersion = newConfig.get("spec").get("postgres").get("version").asText();
    assertEquals(StackGresComponent.POSTGRESQL.getLatest().getLatestVersion(),
        actualPostgresVersion);
  }

  @Test
  void clusteWithLatestPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(StackGresComponent.LATEST);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newConfig = jp.apply(crJson);

    String actualPostgresVersion = newConfig.get("spec").get("postgres").get("version").asText();
    assertEquals(StackGresComponent.POSTGRESQL.getLatest().getLatestVersion(),
        actualPostgresVersion);
  }

  @Test
  void clusteWithMajorPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    review.getRequest().getObject().getSpec().getPostgres().setVersion("12");

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonNode crJson = JSON_MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newConfig = jp.apply(crJson);

    String actualPostgresVersion = newConfig.get("spec").get("postgres").get("version").asText();
    assertEquals(StackGresComponent.POSTGRESQL.getLatest().getVersion("12"),
        actualPostgresVersion);
  }
}
