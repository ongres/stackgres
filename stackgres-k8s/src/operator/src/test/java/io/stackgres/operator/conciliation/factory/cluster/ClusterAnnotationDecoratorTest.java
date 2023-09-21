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
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import io.stackgres.common.PatroniUtil;
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

  private final ClusterAnnotationDecorator annotationDecorator = new ClusterAnnotationDecorator();

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

    String serviceAnnotationKey = StringUtil.generateRandom(8);
    String serviceAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(Map.of(serviceAnnotationKey, serviceAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(allResourceAnnotationKey, allResourceAnnotationValue,
        serviceAnnotationKey, serviceAnnotationValue);

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
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

    String podAnnotationKey = StringUtil.generateRandom(8);
    String podAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setClusterPods(Map.of(podAnnotationKey, podAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
        .forEach(resource -> {
          assertFalse(resource.getMetadata().getAnnotations().containsKey(podAnnotationKey));
        });

  }

  @Test
  void primaryServices_shouldHavePrimaryServiceAnnotations() {
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());

    String primaryAnnotationKey = "primary-" + StringUtil.generateRandom(8);
    String primaryAnnotationValue = "primary-" + StringUtil.generateRandom(8);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setPrimaryService(Map.of(primaryAnnotationKey, primaryAnnotationValue));

    String serviceAnnotationKey = "service-" + StringUtil.generateRandom(8);
    String serviceAnnotationValue = "service-" + StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().getPostgresServices().setReplicas(null);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(Map.of(serviceAnnotationKey, serviceAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(primaryAnnotationKey, primaryAnnotationValue,
        serviceAnnotationKey, serviceAnnotationValue);

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
        .filter(r -> r.getMetadata().getName().endsWith(PatroniUtil.DEPRECATED_READ_WRITE_SERVICE))
        .forEach(resource -> checkResourceAnnotations(resource, expected));
  }

  @Test
  void replicaServices_shouldHaveReplicaServiceAnnotations() {
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());

    String replicaAnnotationKey = StringUtil.generateRandom(8);
    String replicaAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setReplicasService(Map.of(replicaAnnotationKey, replicaAnnotationValue));

    String serviceAnnotationKey = StringUtil.generateRandom(8);
    String serviceAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().setPods(null);
    defaultCluster.getSpec().getPostgresServices().setPrimary(null);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(Map.of(serviceAnnotationKey, serviceAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(replicaAnnotationKey, replicaAnnotationValue,
        serviceAnnotationKey, serviceAnnotationValue);

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
        .filter(r -> r.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE))
        .forEach(resource -> checkResourceAnnotations(resource, expected));
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

    String podAnnotationKey = StringUtil.generateRandom(8);
    String podAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setClusterPods(Map.of(podAnnotationKey, podAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(allResourceAnnotationKey, allResourceAnnotationValue,
        podAnnotationKey, podAnnotationValue);

    resources.stream()
        .filter(r -> r.getKind().equals("Pod"))
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

    String serviceAnnotationKey = StringUtil.generateRandom(8);
    String serviceAnnotationValue = StringUtil.generateRandom(8);

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(serviceAnnotationKey, serviceAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    resources.stream()
        .filter(r -> r.getKind().equals("Pod"))
        .forEach(resource -> assertFalse(
            resource.getMetadata().getAnnotations().containsKey(serviceAnnotationKey)));
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

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setClusterPods(Map.of(podAnnotationKey, podAnnotationValue));

    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expectedSts = Map.of(allResourceAnnotationKey, allResourceAnnotationValue);
    Map<String, String> expectedPod = Map.of(allResourceAnnotationKey, allResourceAnnotationValue,
        podAnnotationKey, podAnnotationValue);

    resources.stream()
        .filter(r -> r.getKind().equals("StatefulSet"))
        .forEach(resource -> {
          checkResourceAnnotations(resource, expectedSts);
          StatefulSet statefulSet = (StatefulSet) resource;
          checkResourceAnnotations(statefulSet.getSpec().getTemplate(), expectedPod);
        });

  }

  @Test
  void clusterOperatorVersion_shouldBePresentInStatefulSetPodTemplates() {
    resources.forEach(resource -> annotationDecorator.decorate(context, resource));

    Map<String, String> expected = Map.of(StackGresContext.VERSION_KEY, defaultCluster
        .getMetadata().getAnnotations().get(StackGresContext.VERSION_KEY));

    resources.stream()
        .filter(r -> r.getKind().equals("StatefulSet"))
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
        .filter(r -> r.getKind().equals("StatefulSet"))
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
