/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StringUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.operator.conciliation.factory.cluster.AnnotationDecoratorImpl;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnnotationDecoratorImplTest {

  private final AnnotationDecoratorImpl annotationDecorator = new AnnotationDecoratorImpl();

  private StackGresCluster defaultCluster;

  private List<HasMetadata> resources;

  @BeforeEach
  void setUp() {
    defaultCluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);

    final ObjectMeta metadata = defaultCluster.getMetadata();
    resources = KubernetessMockResourceGenerationUtil
        .buildResources(metadata.getName(), metadata.getNamespace());
  }

  @Test
  void allResources_shouldBeAppliedToAllResources() {

    String randomAnnotationKey = StringUtil.generateRandom();
    String randomAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(randomAnnotationKey, randomAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.forEach(resource -> checkResourceAnnotations(resource,
        new Tuple2<>(randomAnnotationKey, randomAnnotationValue)));

  }

  @Test
  void services_shouldHaveServicesAnnotationsAndAllResourcesAnnotations() {
    String allResourceAnnotationKey = StringUtil.generateRandom();
    String allResourceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String serviceAnnotationKey = StringUtil.generateRandom();
    String serviceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(serviceAnnotationKey, serviceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
        .forEach(resource -> checkResourceAnnotations(resource,
            new Tuple2<>(allResourceAnnotationKey, allResourceAnnotationValue),
            new Tuple2<>(serviceAnnotationKey, serviceAnnotationValue)));

  }

  @Test
  void services_shouldNotHavePodAnnotations() {
    String allResourceAnnotationKey = StringUtil.generateRandom();
    String allResourceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String podAnnotationKey = StringUtil.generateRandom();
    String podAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setPods(ImmutableMap.of(podAnnotationKey, podAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
        .forEach(resource -> {
          assertFalse(resource.getMetadata().getAnnotations().containsKey(podAnnotationKey));
        });

  }

  @Test
  void primaryServices_shouldHavePrimaryServiceAnnotations() {
    String primaryAnnotationKey = StringUtil.generateRandom();
    String primaryAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().getPostgresServices().getPrimary()
        .setAnnotations(ImmutableMap.of(primaryAnnotationKey, primaryAnnotationValue));

    String serviceAnnotationKey = StringUtil.generateRandom();
    String serviceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().getPostgresServices().setReplicas(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(serviceAnnotationKey, serviceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
        .filter(r -> r.getMetadata().getName().endsWith(PatroniUtil.READ_WRITE_SERVICE))
        .forEach(resource -> checkResourceAnnotations(resource,
            new Tuple2<>(primaryAnnotationKey, primaryAnnotationValue),
            new Tuple2<>(serviceAnnotationKey, serviceAnnotationValue)
        ));

  }

  @Test
  void replicaServices_shouldHaveReplicaServiceAnnotations() {
    String replicaAnnotationKey = StringUtil.generateRandom();
    String replicaAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().getPostgresServices().getReplicas()
        .setAnnotations(ImmutableMap.of(replicaAnnotationKey, replicaAnnotationValue));

    String serviceAnnotationKey = StringUtil.generateRandom();
    String serviceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().getPostgresServices().setPrimary(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(serviceAnnotationKey, serviceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
        .filter(r -> r.getMetadata().getName().endsWith(PatroniUtil.READ_ONLY_SERVICE))
        .forEach(resource -> checkResourceAnnotations(resource,
            new Tuple2<>(replicaAnnotationKey, replicaAnnotationValue),
            new Tuple2<>(serviceAnnotationKey, serviceAnnotationValue)
        ));

  }

  @Test
  void pods_shouldHavePodAnnotationsAndAllResourcesAnnotations() {
    String allResourceAnnotationKey = StringUtil.generateRandom();
    String allResourceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String podAnnotationKey = StringUtil.generateRandom();
    String podAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setPods(ImmutableMap.of(podAnnotationKey, podAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Pod"))
        .forEach(resource -> checkResourceAnnotations(resource,
            new Tuple2<>(allResourceAnnotationKey, allResourceAnnotationValue),
            new Tuple2<>(podAnnotationKey, podAnnotationValue)));

  }

  @Test
  void pods_shouldNotHaveServiceAnnotations() {
    String allResourceAnnotationKey = StringUtil.generateRandom();
    String allResourceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String serviceAnnotationKey = StringUtil.generateRandom();
    String serviceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(serviceAnnotationKey, serviceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Pod"))
        .forEach(resource ->
            assertFalse(resource.getMetadata().getAnnotations().containsKey(serviceAnnotationKey)));

  }

  @Test
  void podsAnnotations_shouldBePresentInStatefulSetPodTemplates() {
    String allResourceAnnotationKey = StringUtil.generateRandom();
    String allResourceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String podAnnotationKey = StringUtil.generateRandom();
    String podAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setPods(ImmutableMap.of(podAnnotationKey, podAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("StatefulSet"))
        .forEach(resource -> {
          checkResourceAnnotations(resource,
              new Tuple2<>(allResourceAnnotationKey, allResourceAnnotationValue));
          StatefulSet statefulSet = (StatefulSet) resource;
          checkResourceAnnotations(statefulSet.getSpec().getTemplate(),
              new Tuple2<>(podAnnotationKey, podAnnotationValue));
        });

  }

  @Test
  void clusterOperatorVersion_shouldBePresentInStatefulSetPodTemplates() {

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("StatefulSet"))
        .forEach(resource -> {
          StatefulSet statefulSet = (StatefulSet) resource;
          checkResourceAnnotations(statefulSet.getSpec().getTemplate(),
              new Tuple2<>(StackGresContext.VERSION_KEY, defaultCluster
                  .getMetadata().getAnnotations().get(StackGresContext.VERSION_KEY)));
        });

  }

  @Test
  void allResourcesAnnotations_shouldBePresentInStatefulSetPersistenVolumeClaims() {
    String allResourceAnnotationKey = StringUtil.generateRandom();
    String allResourceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("StatefulSet"))
        .forEach(resource -> {
          StatefulSet statefulSet = (StatefulSet) resource;
          statefulSet.getSpec().getVolumeClaimTemplates().forEach(template -> {
            checkResourceAnnotations(template,
                new Tuple2<>(allResourceAnnotationKey, allResourceAnnotationValue));
          });
        });
  }

  @Test
  void allResourcesAnnotations_shouldNotBePresentInStatefulSetPersistenVolumeClaimsIfStatefulSetAlreadyExists() {
    String allResourceAnnotationKey = StringUtil.generateRandom();
    String allResourceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(
        new StatefulSetBuilder()
        .withNewMetadata().withNamespace("test").withName("testStatefulSet").endMetadata()
        .build()), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("StatefulSet"))
        .forEach(resource -> {
          StatefulSet statefulSet = (StatefulSet) resource;
          statefulSet.getSpec().getVolumeClaimTemplates().forEach(this::checkResourceAnnotations);
        });
  }

  @Test
  void allResourcesAnnotations_shouldBePresentInCronJobsPodTemplate() {
    String allResourceAnnotationKey = StringUtil.generateRandom();
    String allResourceAnnotationValue = StringUtil.generateRandom();

    defaultCluster.getSpec().setPod(null);
    defaultCluster.getSpec().setPostgresServices(null);
    defaultCluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, ImmutableList.of(), resources);

    resources.stream()
        .filter(r -> r.getKind().equals("CronJob"))
        .forEach(resource -> {
          CronJob cronJob = (CronJob) resource;
          final JobTemplateSpec jobTemplate = cronJob.getSpec().getJobTemplate();
          checkResourceAnnotations(jobTemplate,
              new Tuple2<>(allResourceAnnotationKey, allResourceAnnotationValue));
          PodTemplateSpec template = jobTemplate.getSpec().getTemplate();
          checkResourceAnnotations(template,
              new Tuple2<>(allResourceAnnotationKey, allResourceAnnotationValue));
        });
  }

  @SafeVarargs
  private final void checkResourceAnnotations(HasMetadata resource,
                                              Tuple2<String, String>... annotations) {
    assertEquals(annotations.length, Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .map(Map::size)
        .orElse(0));

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElse(ImmutableMap.of());

    Arrays.asList(annotations).forEach(annotation -> {
      assertTrue(resourceAnnotation.containsKey(annotation.v1));
      assertEquals(annotation.v2, resourceAnnotation.get(annotation.v1));
    });

  }

  @SafeVarargs
  private final void checkResourceAnnotations(PodTemplateSpec resource,
                                              Tuple2<String, String>... annotations) {

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElseGet(() -> fail("No annotations found for resource " + resource.toString()));

    Arrays.asList(annotations).forEach(annotation -> {
      assertTrue(resourceAnnotation.containsKey(annotation.v1));
      assertEquals(annotation.v2, resourceAnnotation.get(annotation.v1));
    });

  }

  @SafeVarargs
  private final void checkResourceAnnotations(JobTemplateSpec resource,
                                              Tuple2<String, String>... annotations) {

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElseGet(() -> fail("No annotations found for resource " + resource.toString()));

    Arrays.asList(annotations).forEach(annotation -> {
      assertTrue(resourceAnnotation.containsKey(annotation.v1));
      assertEquals(annotation.v2, resourceAnnotation.get(annotation.v1));
    });

  }

}