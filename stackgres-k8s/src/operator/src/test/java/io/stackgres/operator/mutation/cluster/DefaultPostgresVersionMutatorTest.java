/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPostgresVersionMutatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  protected static final ObjectMapper MAPPER = JsonUtil.JSON_MAPPER;

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresClusterReview review;

  private DefaultPostgresVersionMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    mutator = new DefaultPostgresVersionMutator();
    mutator.setObjectMapper(MAPPER);
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

    JsonNode crJson = MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newConfig = jp.apply(crJson);

    String actualPostgresVersion = newConfig.get("spec").get("postgres").get("version").asText();
    assertEquals(StackGresComponent.POSTGRESQL.getLatest().getLatestVersion(),
        actualPostgresVersion);
  }

  @Test
  void clusteWithNoPostgresFlavor_shouldSetFinalValue() throws JsonPatchException {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().getPostgres().setFlavor(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonNode crJson = MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newConfig = jp.apply(crJson);

    String actualPostgresFlavor = newConfig.get("spec").get("postgres").get("flavor").asText();
    assertEquals(StackGresPostgresFlavor.VANILLA.toString(), actualPostgresFlavor);
  }

  @Test
  void clusteWithLatestPostgresVersion_shouldSetFinalValue() throws JsonPatchException {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(StackGresComponent.LATEST);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    JsonNode crJson = MAPPER.valueToTree(review.getRequest().getObject());

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

    JsonNode crJson = MAPPER.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newConfig = jp.apply(crJson);

    String actualPostgresVersion = newConfig.get("spec").get("postgres").get("version").asText();
    assertEquals(StackGresComponent.POSTGRESQL.getLatest().getVersion("12"),
        actualPostgresVersion);
  }
}
