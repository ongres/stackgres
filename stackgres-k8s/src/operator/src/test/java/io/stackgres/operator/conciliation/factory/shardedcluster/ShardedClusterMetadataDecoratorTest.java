/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecAnnotations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecLabels;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpecMetadata;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterMetadataDecoratorTest {

  private final ShardedClusterMetadataDecorator decorator = new ShardedClusterMetadataDecorator();

  @Mock
  private StackGresShardedClusterContext context;

  private StackGresShardedCluster shardedCluster;

  @BeforeEach
  void setUp() {
    shardedCluster = Fixtures.shardedCluster().loadDefault().get();

    lenient().when(context.getShardedCluster()).thenReturn(shardedCluster);
    lenient().when(context.getSource()).thenReturn(shardedCluster);

    shardedCluster.getMetadata().getAnnotations()
        .put(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString());
  }

  @Test
  void decorate_whenAllResourcesAnnotationsSet_shouldPropagateToResource() {
    String annotationKey = "custom-annotation-key";
    String annotationValue = "custom-annotation-value";

    setupMetadataWithAnnotations(Map.of(annotationKey, annotationValue));

    HasMetadata configMap = new ConfigMapBuilder()
        .withNewMetadata().withName("test-cm").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, configMap);

    assertNotNull(configMap.getMetadata().getAnnotations());
    assertEquals(annotationValue,
        configMap.getMetadata().getAnnotations().get(annotationKey));
  }

  @Test
  void decorate_whenAllResourcesLabelsSet_shouldPropagateToResource() {
    String labelKey = "custom-label-key";
    String labelValue = "custom-label-value";

    setupMetadataWithLabels(Map.of(labelKey, labelValue));

    HasMetadata service = new ServiceBuilder()
        .withNewMetadata().withName("test-svc").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, service);

    assertNotNull(service.getMetadata().getLabels());
    assertEquals(labelValue, service.getMetadata().getLabels().get(labelKey));
  }

  @Test
  void decorate_whenAnnotationsAndLabelsSet_shouldPropagatesBothToResource() {
    String annotationKey = "ann-key";
    String annotationValue = "ann-value";
    String labelKey = "label-key";
    String labelValue = "label-value";

    StackGresShardedClusterSpecMetadata specMetadata = new StackGresShardedClusterSpecMetadata();
    StackGresShardedClusterSpecAnnotations annotations =
        new StackGresShardedClusterSpecAnnotations();
    annotations.setAllResources(Map.of(annotationKey, annotationValue));
    specMetadata.setAnnotations(annotations);
    StackGresShardedClusterSpecLabels labels = new StackGresShardedClusterSpecLabels();
    labels.setAllResources(Map.of(labelKey, labelValue));
    specMetadata.setLabels(labels);
    shardedCluster.getSpec().setMetadata(specMetadata);

    HasMetadata configMap = new ConfigMapBuilder()
        .withNewMetadata().withName("test-cm").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, configMap);

    assertNotNull(configMap.getMetadata().getAnnotations());
    assertTrue(configMap.getMetadata().getAnnotations().containsKey(annotationKey));
    assertNotNull(configMap.getMetadata().getLabels());
    assertTrue(configMap.getMetadata().getLabels().containsKey(labelKey));
  }

  @Test
  void decorate_whenMetadataSpecIsNull_shouldStillDecorateWithVersionAnnotation() {
    shardedCluster.getSpec().setMetadata(null);

    HasMetadata configMap = new ConfigMapBuilder()
        .withNewMetadata().withName("test-cm").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, configMap);

    assertNotNull(configMap.getMetadata().getAnnotations());
    assertTrue(configMap.getMetadata().getAnnotations()
        .containsKey(StackGresContext.VERSION_KEY));
  }

  @Test
  void decorate_whenAnnotationsIsNull_shouldStillDecorateWithVersionAnnotation() {
    StackGresShardedClusterSpecMetadata specMetadata = new StackGresShardedClusterSpecMetadata();
    specMetadata.setAnnotations(null);
    shardedCluster.getSpec().setMetadata(specMetadata);

    HasMetadata configMap = new ConfigMapBuilder()
        .withNewMetadata().withName("test-cm").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, configMap);

    assertNotNull(configMap.getMetadata().getAnnotations());
    assertTrue(configMap.getMetadata().getAnnotations()
        .containsKey(StackGresContext.VERSION_KEY));
  }

  @Test
  void decorate_whenLabelsIsNull_shouldNotSetLabels() {
    shardedCluster.getSpec().setMetadata(null);

    HasMetadata configMap = new ConfigMapBuilder()
        .withNewMetadata().withName("test-cm").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, configMap);

    assertTrue(configMap.getMetadata().getLabels().isEmpty());
  }

  @Test
  void decorate_whenStatefulSet_shouldPropagateAnnotationsToPodTemplateAndVolumeClaimTemplates() {
    String annotationKey = "custom-ann";
    String annotationValue = "custom-val";

    setupMetadataWithAnnotations(Map.of(annotationKey, annotationValue));

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata().withName("test-sts").withNamespace("test-ns").endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewMetadata().endMetadata()
        .withNewSpec().endSpec()
        .endTemplate()
        .addNewVolumeClaimTemplate()
        .withNewMetadata().withName("data").endMetadata()
        .withNewSpec().withAccessModes("ReadWriteOnce").endSpec()
        .endVolumeClaimTemplate()
        .endSpec()
        .build();

    decorator.decorate(context, statefulSet);

    assertNotNull(statefulSet.getMetadata().getAnnotations());
    assertTrue(statefulSet.getMetadata().getAnnotations().containsKey(annotationKey));

    ObjectMeta podTemplateMeta = statefulSet.getSpec().getTemplate().getMetadata();
    assertNotNull(podTemplateMeta.getAnnotations());
    assertTrue(podTemplateMeta.getAnnotations().containsKey(annotationKey));

    ObjectMeta pvcMeta = statefulSet.getSpec().getVolumeClaimTemplates()
        .getFirst().getMetadata();
    assertNotNull(pvcMeta.getAnnotations());
    assertTrue(pvcMeta.getAnnotations().containsKey(annotationKey));
  }

  @Test
  void decorate_whenStatefulSet_shouldPropagateLabelsToTemplates() {
    String labelKey = "custom-label";
    String labelValue = "custom-label-val";

    setupMetadataWithLabels(Map.of(labelKey, labelValue));

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata().withName("test-sts").withNamespace("test-ns").endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewMetadata().endMetadata()
        .withNewSpec().endSpec()
        .endTemplate()
        .addNewVolumeClaimTemplate()
        .withNewMetadata().withName("data").endMetadata()
        .withNewSpec().withAccessModes("ReadWriteOnce").endSpec()
        .endVolumeClaimTemplate()
        .endSpec()
        .build();

    decorator.decorate(context, statefulSet);

    ObjectMeta podTemplateMeta = statefulSet.getSpec().getTemplate().getMetadata();
    assertNotNull(podTemplateMeta.getLabels());
    assertTrue(podTemplateMeta.getLabels().containsKey(labelKey));

    ObjectMeta pvcMeta = statefulSet.getSpec().getVolumeClaimTemplates()
        .getFirst().getMetadata();
    assertNotNull(pvcMeta.getLabels());
    assertTrue(pvcMeta.getLabels().containsKey(labelKey));
  }

  @Test
  void decorate_whenResourceHasExistingAnnotations_shouldMergeWithCustomAnnotations() {
    String customAnnotationKey = "custom-key";
    String customAnnotationValue = "custom-value";
    String existingAnnotationKey = "existing-key";
    String existingAnnotationValue = "existing-value";

    setupMetadataWithAnnotations(Map.of(customAnnotationKey, customAnnotationValue));

    HasMetadata configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withName("test-cm")
        .withNamespace("test-ns")
        .withAnnotations(Map.of(existingAnnotationKey, existingAnnotationValue))
        .endMetadata()
        .build();

    decorator.decorate(context, configMap);

    assertNotNull(configMap.getMetadata().getAnnotations());
    assertEquals(existingAnnotationValue,
        configMap.getMetadata().getAnnotations().get(existingAnnotationKey));
    assertEquals(customAnnotationValue,
        configMap.getMetadata().getAnnotations().get(customAnnotationKey));
  }

  @Test
  void decorate_whenResourceHasExistingLabels_shouldMergeWithCustomLabels() {
    String customLabelKey = "custom-label";
    String customLabelValue = "custom-label-value";
    String existingLabelKey = "existing-label";
    String existingLabelValue = "existing-label-value";

    setupMetadataWithLabels(Map.of(customLabelKey, customLabelValue));

    HasMetadata configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withName("test-cm")
        .withNamespace("test-ns")
        .withLabels(Map.of(existingLabelKey, existingLabelValue))
        .endMetadata()
        .build();

    decorator.decorate(context, configMap);

    assertNotNull(configMap.getMetadata().getLabels());
    assertEquals(existingLabelValue,
        configMap.getMetadata().getLabels().get(existingLabelKey));
    assertEquals(customLabelValue,
        configMap.getMetadata().getLabels().get(customLabelKey));
  }

  @Test
  void decorate_whenVersionAnnotation_shouldBePresent() {
    setupMetadataWithAnnotations(Map.of());

    HasMetadata configMap = new ConfigMapBuilder()
        .withNewMetadata().withName("test-cm").withNamespace("test-ns").endMetadata()
        .build();

    decorator.decorate(context, configMap);

    assertNotNull(configMap.getMetadata().getAnnotations());
    assertTrue(configMap.getMetadata().getAnnotations()
        .containsKey(StackGresContext.VERSION_KEY));
  }

  private void setupMetadataWithAnnotations(Map<String, String> allResourceAnnotations) {
    StackGresShardedClusterSpecMetadata specMetadata =
        shardedCluster.getSpec().getMetadata() != null
            ? shardedCluster.getSpec().getMetadata()
            : new StackGresShardedClusterSpecMetadata();
    StackGresShardedClusterSpecAnnotations annotations = specMetadata.getAnnotations() != null
        ? specMetadata.getAnnotations()
        : new StackGresShardedClusterSpecAnnotations();
    annotations.setAllResources(allResourceAnnotations);
    specMetadata.setAnnotations(annotations);
    shardedCluster.getSpec().setMetadata(specMetadata);
  }

  private void setupMetadataWithLabels(Map<String, String> allResourceLabels) {
    StackGresShardedClusterSpecMetadata specMetadata =
        shardedCluster.getSpec().getMetadata() != null
            ? shardedCluster.getSpec().getMetadata()
            : new StackGresShardedClusterSpecMetadata();
    StackGresShardedClusterSpecLabels labels = specMetadata.getLabels() != null
        ? specMetadata.getLabels()
        : new StackGresShardedClusterSpecLabels();
    labels.setAllResources(allResourceLabels);
    specMetadata.setLabels(labels);
    shardedCluster.getSpec().setMetadata(specMetadata);
  }

}
