/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterAnnotationDecoratorTest {

  private final ClusterMetadataDecorator annotationDecorator = new ClusterMetadataDecorator();

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster defaultCluster;

  private List<HasMetadata> resources;

  @BeforeEach
  void setUp() {
    defaultCluster = Fixtures.cluster().loadDefault().get();

    when(context.getSource()).thenReturn(defaultCluster);

    final ObjectMeta metadata = defaultCluster.getMetadata();
    metadata.getAnnotations().put(StackGresContext.VERSION_KEY,
        StackGresProperty.OPERATOR_VERSION.getString());
    resources = KubernetessMockResourceGenerationUtil
        .buildResources(metadata.getName(), metadata.getNamespace());
  }

  @Test
  void allResources_shouldBeAppliedToAllResources() {

    String randomAnnotationKey = StringUtil.generateRandom(8);
    String randomAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(Map.of(randomAnnotationKey, randomAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    resources.forEach(resource -> checkResourceAnnotations(resource,
        Map.of(randomAnnotationKey, randomAnnotationValue)));

  }

  @Test
  void services_shouldHaveServicesAnnotationsAndAllResourcesAnnotations() {
    String allResourceAnnotationKey = StringUtil.generateRandom(8);
    String allResourceAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(Map.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String annotationKey = StringUtil.generateRandom(8);
    String annotationValue = StringUtil.generateRandom(8);

    resources.forEach(resource -> resource.getMetadata().setAnnotations(
        Map.of(annotationKey, annotationValue)));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(allResourceAnnotationKey, allResourceAnnotationValue,
        annotationKey, annotationValue);

    resources.stream()
        .filter(Service.class::isInstance)
        .forEach(resource -> checkResourceAnnotations(resource, expected));
  }

  @Test
  void services_shouldNotHavePodAnnotations() {
    String allResourceAnnotationKey = StringUtil.generateRandom(8);
    String allResourceAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String annotationKey = StringUtil.generateRandom(8);
    String annotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setClusterPods(Map.of(annotationKey, annotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    resources.stream()
        .filter(Service.class::isInstance)
        .forEach(resource -> {
          assertFalse(resource.getMetadata().getAnnotations().containsKey(annotationKey));
        });

  }

  @Test
  void pods_shouldHavePodAnnotationsAndAllResourcesAnnotations() {
    String allResourceAnnotationKey = StringUtil.generateRandom(8);
    String allResourceAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String annotationKey = StringUtil.generateRandom(8);
    String annotationValue = StringUtil.generateRandom(8);

    resources.forEach(resource -> resource.getMetadata().setAnnotations(
        Map.of(annotationKey, annotationValue)));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(allResourceAnnotationKey, allResourceAnnotationValue,
        annotationKey, annotationValue);

    resources.stream()
        .filter(Pod.class::isInstance)
        .forEach(resource -> checkVersionableResourceAnnotations(resource, expected));
  }

  @Test
  void pods_shouldNotHaveServiceAnnotations() {
    String allResourceAnnotationKey = StringUtil.generateRandom(8);
    String allResourceAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String annotationKey = StringUtil.generateRandom(8);
    String annotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(annotationKey, annotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    resources.stream()
        .filter(Pod.class::isInstance)
        .forEach(resource -> assertFalse(
            resource.getMetadata().getAnnotations().containsKey(annotationKey)));
  }

  @Test
  void podsAnnotations_shouldBePresentInStatefulSetPodTemplates() {
    String allResourceAnnotationKey = "AllResource-" + StringUtil.generateRandom(8);
    String allResourceAnnotationValue = "AllResource-" + StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(Map.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String podAnnotationKey = "Pod-" + StringUtil.generateRandom(8);
    String podAnnotationValue = "Pod-" + StringUtil.generateRandom(8);

    resources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .forEach(statefulSet -> statefulSet.getSpec().getTemplate().getMetadata().setAnnotations(
            Map.of(podAnnotationKey, podAnnotationValue)));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expectedSts = Map.of(allResourceAnnotationKey, allResourceAnnotationValue);
    Map<String, String> expected = Map.of(allResourceAnnotationKey, allResourceAnnotationValue,
        podAnnotationKey, podAnnotationValue);

    resources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .forEach(statefulSet -> {
          checkResourceAnnotations(statefulSet, expectedSts);
          checkResourceAnnotations(statefulSet.getSpec().getTemplate(), expected);
        });
  }

  @Test
  void pvcsAnnotations_shouldBePresentInStatefulSetPodTemplates() {
    String allResourceAnnotationKey = "AllResource-" + StringUtil.generateRandom(8);
    String allResourceAnnotationValue = "AllResource-" + StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(Map.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String pvcAnnotationKey = "Pvc-" + StringUtil.generateRandom(8);
    String pvcAnnotationValue = "Pvc-" + StringUtil.generateRandom(8);

    resources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .forEach(statefulSet -> statefulSet.getSpec().getVolumeClaimTemplates()
            .forEach(volumeClaimTemplate -> volumeClaimTemplate.getMetadata().setAnnotations(
                Map.of(pvcAnnotationKey, pvcAnnotationValue))));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expectedSts = Map.of(allResourceAnnotationKey, allResourceAnnotationValue);
    Map<String, String> expected = Map.of(allResourceAnnotationKey, allResourceAnnotationValue,
        pvcAnnotationKey, pvcAnnotationValue);

    resources.stream()
        .filter(StatefulSet.class::isInstance)
        .map(StatefulSet.class::cast)
        .forEach(statefulSet -> {
          checkResourceAnnotations(statefulSet, expectedSts);
          statefulSet.getSpec().getVolumeClaimTemplates()
              .forEach(volumeClaimTemplate -> checkResourceAnnotations(volumeClaimTemplate, expected));
        });
  }

  @Test
  void clusterOperatorVersion_shouldBePresentInStatefulSetPodTemplates() {
    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(StackGresContext.VERSION_KEY, defaultCluster
        .getMetadata().getAnnotations().get(StackGresContext.VERSION_KEY));

    resources.stream()
        .filter(StatefulSet.class::isInstance)
        .forEach(resource -> {
          StatefulSet statefulSet = (StatefulSet) resource;
          checkResourceAnnotations(statefulSet.getSpec().getTemplate(), expected);
        });
  }

  @Test
  void allResourcesAnnotations_shouldBePresentInStatefulSetPersistenVolumeClaims() {
    String allResourceAnnotationKey = StringUtil.generateRandom(8);
    String allResourceAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(allResourceAnnotationKey, allResourceAnnotationValue);

    resources.stream()
        .filter(StatefulSet.class::isInstance)
        .forEach(resource -> {
          StatefulSet statefulSet = (StatefulSet) resource;
          statefulSet.getSpec().getVolumeClaimTemplates().forEach(template -> {
            checkResourceAnnotations(template, expected);
          });
        });
  }

  @Test
  void allResourcesAnnotations_shouldBePresentInCronJobsPodTemplate() {
    String allResourceAnnotationKey = StringUtil.generateRandom(8);
    String allResourceAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(allResourceAnnotationKey, allResourceAnnotationValue);

    resources.stream()
        .filter(CronJob.class::isInstance)
        .map(CronJob.class::cast)
        .forEach(cronJob -> {
          final JobTemplateSpec jobTemplate = cronJob.getSpec().getJobTemplate();
          checkResourceAnnotations(jobTemplate, expected);
          PodTemplateSpec template = jobTemplate.getSpec().getTemplate();
          checkResourceAnnotations(template, expected);
        });
  }

  private final void checkVersionableResourceAnnotations(HasMetadata resource,
      Map<String, String> annotations) {
    ImmutableMap<String, String> expectedAnnotations = ImmutableMap.<String, String>builder()
        .putAll(annotations)
        .put(StackGresContext.VERSION_KEY, StackGresProperty.OPERATOR_VERSION.getString())
        .build();

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElse(Map.of());

    assertThat(resourceAnnotation).containsExactlyEntriesIn(expectedAnnotations);
  }

  private final void checkResourceAnnotations(HasMetadata resource,
      Map<String, String> annotations) {

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElse(Map.of());

    assertThat(resourceAnnotation).containsAtLeastEntriesIn(annotations);
  }

  private final void checkResourceAnnotations(PodTemplateSpec resource,
      Map<String, String> annotations) {

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElseGet(() -> fail("No annotations found for resource " + resource.toString()));

    assertThat(resourceAnnotation).containsAtLeastEntriesIn(annotations);
  }

  private final void checkResourceAnnotations(JobTemplateSpec resource,
      Map<String, String> annotations) {

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElseGet(() -> fail("No annotations found for resource " + resource.toString()));

    assertThat(resourceAnnotation).containsAtLeastEntriesIn(annotations);
  }

}
