/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StringUtil;
import io.stackgres.operator.conciliation.comparator.StackGresAbstractComparator;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Test;

public abstract class ConciliatorTest<T extends CustomResource<?, ?>> {

  private static final ObjectMapper MAPPER = JsonUtil.JSON_MAPPER;

  protected ComparisonDelegator<T> resourceComparator = new ComparisonDelegator<>() {
    private final StackGresAbstractComparator comparator = new StackGresAbstractComparator() {
      @Override
      protected IgnorePatch[] getPatchPattersToIgnore() {
        return new IgnorePatch[0];
      }
    };

    @Override
    public boolean isTheSameResource(HasMetadata required, HasMetadata deployed) {

      return comparator.isTheSameResource(required, deployed);
    }

    @Override
    public boolean isResourceContentEqual(HasMetadata required, HasMetadata deployed) {
      return comparator.isResourceContentEqual(required, deployed);
    }

    @Override
    public ArrayNode getJsonDiff(HasMetadata required, HasMetadata deployed) {
      return comparator.getJsonDiff(required, deployed);
    }
  };

  @Test
  void nonDeployedResources_shouldAppearInTheCreation() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    final List<HasMetadata> deployedResources = new ArrayList<>(requiredResources);

    int indexToRemove = new Random().nextInt(deployedResources.size());
    HasMetadata deletedResource = deployedResources.remove(indexToRemove);

    Conciliator<T> conciliator = buildConciliator(requiredResources,
        deployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());
    assertEquals(1, result.getCreations().size());
    assertEquals(deletedResource, result.getCreations().get(0));

    assertFalse(result.isUpToDate());
  }

  @Test
  void resourceToDelete_shouldAppearInTheDeletions() {

    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    final List<HasMetadata> deployedResources = new ArrayList<>(requiredResources);

    int indexToRemove = new Random().nextInt(requiredResources.size());
    HasMetadata deletedResource = requiredResources.remove(indexToRemove);

    Conciliator<T> conciliator = buildConciliator(requiredResources,
        deployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());
    assertEquals(1, result.getDeletions().size());
    assertEquals(deletedResource, result.getDeletions().get(0));

    assertFalse(result.isUpToDate());
  }

  @Test
  void whenThereIsNoChanges_allResourcesShouldBeEmpty() {

    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    Conciliator<T> conciliator = buildConciliator(requiredResources,
        deepCopy(requiredResources));

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(0, result.getPatches().size());

    assertTrue(result.isUpToDate());
  }

  @Test
  void whenThereIsChanges_shouldBeDetected() {

    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");
    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    int indexToUpdate = new Random().nextInt(requiredResources.size());
    HasMetadata updatedResource = requiredResources.get(indexToUpdate);
    updatedResource.getMetadata()
        .setLabels(ImmutableMap.of(StringUtil.generateRandom(), StringUtil.generateRandom()));

    Conciliator<T> conciliator = buildConciliator(requiredResources, deployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());

    assertEquals(updatedResource, result.getPatches().get(0).v1);

  }

  @Test
  void conciliation_shouldDetectAllChanges() {

    Conciliator<T> conciliator = buildConciliator(
        KubernetessMockResourceGenerationUtil
            .buildResources("test", "test"),
        KubernetessMockResourceGenerationUtil
            .buildResources("test", "test"));

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());
    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(KubernetessMockResourceGenerationUtil
            .buildResources("test", "test").size(),
        result.getPatches().size());

  }

  @Test
  void conciliation_shouldDetectStatefulSetChanges() {
    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");
    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    deployedResources.stream().filter(dr -> dr instanceof StatefulSet)
        .map(sts -> (StatefulSet) sts)
        .forEach(sts -> sts.getSpec().setReplicas(10));

    Conciliator<T> conciliator = buildConciliator(requiredResources, deployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());

    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(1, result.getPatches().size());
  }

  @Test
  void conciliation_shouldIgnoreChangesOnResourcesMarkedWithReconciliationPauseAnnotatinon() {

    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");
    final List<HasMetadata> deployedResources = deepCopy(requiredResources);

    deployedResources.stream().findAny()
        .orElseThrow().getMetadata().setAnnotations(Map.of(
            StackGresContext.RECONCILIATION_PAUSE_KEY, Boolean.TRUE.toString()));

    Conciliator<T> conciliator = buildConciliator(requiredResources, deployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());

    assertEquals(0, result.getDeletions().size());
    assertEquals(0, result.getCreations().size());
    assertEquals(0, result.getPatches().size());

    assertTrue(result.isUpToDate());

  }

  @Test
  void conciliation_shouldIgnoreDeletionsOnResourcesMarkedWithReconciliationPauseAnnotation() {

    final List<HasMetadata> requiredResources = KubernetessMockResourceGenerationUtil
        .buildResources("test", "test");

    final List<HasMetadata> deployedResources = new ArrayList<>(requiredResources);

    int indexToRemove = new Random().nextInt(requiredResources.size() / 2 - 1);
    deployedResources.get(indexToRemove).getMetadata().setAnnotations(Map.of(
        StackGresContext.RECONCILIATION_PAUSE_KEY,
        Boolean.TRUE.toString()));

    int indexToChangeFalsePause = new Random().nextInt(requiredResources.size() / 2)
        + requiredResources.size() / 2 + 1;
    deployedResources.get(indexToChangeFalsePause).getMetadata().setAnnotations(Map.of(
        StackGresContext.RECONCILIATION_PAUSE_KEY,
        Boolean.FALSE.toString()));

    requiredResources.remove(indexToRemove);

    Conciliator<T> conciliator = buildConciliator(requiredResources,
        deployedResources);

    ReconciliationResult result = conciliator.evalReconciliationState(getConciliationResource());
    assertEquals(0, result.getDeletions().size());

    assertTrue(result.isUpToDate());

  }

  protected abstract Conciliator<T> buildConciliator(
      List<HasMetadata> required, List<HasMetadata> deployed);

  protected abstract T getConciliationResource();

  protected List<HasMetadata> deepCopy(List<HasMetadata> source) {
    return source.stream().map(resource -> {

      try {
        final HasMetadata resourceCopy = MAPPER.readValue(MAPPER
            .writeValueAsString(resource), HasMetadata.class);
        resource.getMetadata().setUid(UUID.randomUUID().toString());
        resource.getMetadata().setResourceVersion(StringUtil.generateRandom());
        return resourceCopy;
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }

    }).collect(Collectors.toUnmodifiableList());
  }
}
