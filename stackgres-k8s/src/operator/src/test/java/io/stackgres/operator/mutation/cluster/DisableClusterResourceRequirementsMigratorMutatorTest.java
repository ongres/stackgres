/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opentest4j.AssertionFailedError;

@ExtendWith(MockitoExtension.class)
class DisableClusterResourceRequirementsMigratorMutatorTest {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  private StackGresClusterReview review;
  private DisableClusterResourceRequirementsMigratorMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();

    mutator = new DisableClusterResourceRequirementsMigratorMutator();
    mutator.init();
  }

  @Test
  void cluster_shouldSetNothing() {
    StackGresCluster actualCluster = mutate(review);
    assertEquals(review.getRequest().getObject(), actualCluster);
  }

  @Test
  void oldClusterWithoutNonProductionOptions_shouldSetIt() {
    final StackGresCluster cluster = review.getRequest().getObject();
    cluster.getMetadata().setAnnotations(Map.of(
        StackGresContext.VERSION_KEY, StackGresVersion.V_1_2.getVersion()));
    cluster.getSpec().setNonProductionOptions(null);

    final StackGresCluster actualCluster = mutate(review);

    assertNotNull(
        actualCluster.getSpec().getNonProductionOptions());
    assertTrue(
        actualCluster.getSpec().getNonProductionOptions().getDisableClusterResourceRequirements());
  }

  @Test
  void oldClusterWithNonProductionOptions_shouldSetItAndPreserveOtherFields() {
    final StackGresCluster cluster = review.getRequest().getObject();
    cluster.getMetadata().setAnnotations(Map.of(
        StackGresContext.VERSION_KEY, StackGresVersion.V_1_2.getVersion()));
    cluster.getSpec().setNonProductionOptions(new StackGresClusterNonProduction());
    cluster.getSpec().getNonProductionOptions().setDisablePatroniResourceRequirements(true);

    final StackGresCluster actualCluster = mutate(review);

    assertNotNull(
        actualCluster.getSpec().getNonProductionOptions());
    assertTrue(
        actualCluster.getSpec().getNonProductionOptions().getDisablePatroniResourceRequirements());
    assertTrue(
        actualCluster.getSpec().getNonProductionOptions().getDisableClusterResourceRequirements());
  }

  @Test
  void oldClusterWithDisableClusterResourceRequirements_shouldSetItAndPreserveOtherFields() {
    final StackGresCluster cluster = review.getRequest().getObject();
    cluster.getMetadata().setAnnotations(Map.of(
        StackGresContext.VERSION_KEY, StackGresVersion.V_1_2.getVersion()));
    cluster.getSpec().setNonProductionOptions(new StackGresClusterNonProduction());
    cluster.getSpec().getNonProductionOptions().setDisablePatroniResourceRequirements(true);
    cluster.getSpec().getNonProductionOptions().setDisableClusterResourceRequirements(false);

    final StackGresCluster actualCluster = mutate(review);

    assertNotNull(
        actualCluster.getSpec().getNonProductionOptions());
    assertTrue(
        actualCluster.getSpec().getNonProductionOptions().getDisablePatroniResourceRequirements());
    assertTrue(
        actualCluster.getSpec().getNonProductionOptions().getDisableClusterResourceRequirements());
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
