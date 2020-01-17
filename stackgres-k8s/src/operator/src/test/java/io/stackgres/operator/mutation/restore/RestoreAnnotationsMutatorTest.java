/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.restore;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableMap;
import io.stackgres.operator.WithRestoreReviewResources;
import io.stackgres.operator.common.ConfigLoader;
import io.stackgres.operator.common.RestoreConfigReview;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.mutation.DefaultAnnotationMutator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RestoreAnnotationsMutatorTest implements WithRestoreReviewResources {

  protected static final ObjectMapper mapper = new ObjectMapper();

  private static ConfigLoader configLoader = new ConfigLoader();

  private static RestoreAnnotationsMutator mutator = new RestoreAnnotationsMutator();

  Map<String, String> defaultAnnotations = mutator.getDefaultAnnotationValues(configLoader);

  @BeforeAll
  static void beforeAll() {
    mutator.setConfigContext(configLoader);
  }

  @Test
  void givenACreationWithEmptyAnnotations_itShouldAddTheDefaultAnnotations() throws JsonPatchException, JsonProcessingException {

    RestoreConfigReview review = getCreationReview();
    review.getRequest().getObject().getMetadata().setAnnotations(ImmutableMap.of());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    StackgresRestoreConfig newCrConfig = applyOperations(review, operations);

    Map<String, String> crAnnotations = newCrConfig.getMetadata().getAnnotations();
    assertEquals(defaultAnnotations.size(), crAnnotations.size());

    assertDefaultAnnotations(crAnnotations);

  }

  @Test
  void givenACreationWithNullAnnotations_itShouldAddTheDefaultAnnotations() throws JsonPatchException, JsonProcessingException {

    RestoreConfigReview review = getCreationReview();
    review.getRequest().getObject().getMetadata().setAnnotations(null);

    List<JsonPatchOperation> operations = mutator.mutate(review);

    StackgresRestoreConfig newCrConfig = applyOperations(review, operations);

    Map<String, String> crAnnotations = newCrConfig.getMetadata().getAnnotations();
    assertEquals(defaultAnnotations.size(), crAnnotations.size());

    assertDefaultAnnotations(crAnnotations);

  }

  @Test
  void givenACreationWithTheDefaultAnnotations_itShouldNotAddAnything() {

    RestoreConfigReview review = getCreationReview();

    ImmutableMap.Builder<String, String> annotationBuilder = ImmutableMap.builder();

    defaultAnnotations.forEach((k, v) -> annotationBuilder
        .put(DefaultAnnotationMutator.STACKGRES_PREFIX + k, v));

    review.getRequest().getObject().getMetadata().setAnnotations(annotationBuilder.build());

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());

  }

  @Test
  void givenACreationWithAnnotationsButNotTheDefaultOnes_itShouldNotAddAnything()
      throws JsonPatchException, JsonProcessingException {

    RestoreConfigReview review = getCreationReview();
    review.getRequest().getObject().getMetadata()
        .setAnnotations(ImmutableMap.of("stackgresIs", "Awesome"));

    List<JsonPatchOperation> operations = mutator.mutate(review);

    StackgresRestoreConfig newCr = applyOperations(review, operations);

    Map<String, String> newAnnotations = newCr.getMetadata().getAnnotations();
    assertDefaultAnnotations(newAnnotations);

    assertEquals(defaultAnnotations.size() + 1, newAnnotations.size());

  }

  @Test
  void givenAnUpdate_itShouldDoAnything() {

    RestoreConfigReview review = getUpdateReview();

    List<JsonPatchOperation> operations = mutator.mutate(review);

    assertTrue(operations.isEmpty());
  }

  private StackgresRestoreConfig applyOperations
      (RestoreConfigReview review, List<JsonPatchOperation> operations)
      throws JsonPatchException, JsonProcessingException {
    JsonNode reviewNode = mapper.valueToTree(review.getRequest().getObject());

    JsonPatch jp = new JsonPatch(operations);
    JsonNode newReviewNode = jp.apply(reviewNode);

    return mapper.treeToValue(newReviewNode, StackgresRestoreConfig.class);
  }

  private void assertDefaultAnnotations(Map<String, String> crAnnotations) {
    defaultAnnotations.forEach((k, v) -> {
      String actualKey = DefaultAnnotationMutator.STACKGRES_PREFIX + k;
      assertTrue(crAnnotations.containsKey(actualKey));

      String crAnnotationValue = crAnnotations.get(actualKey);
      assertEquals(v, crAnnotationValue);

    });
  }
}