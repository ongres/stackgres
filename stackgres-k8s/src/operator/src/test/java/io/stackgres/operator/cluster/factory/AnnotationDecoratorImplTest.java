/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import static io.stackgres.testutil.StringUtils.getRandomString;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import groovy.lang.Tuple2;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.CronJob;
import io.fabric8.kubernetes.api.model.batch.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.JobTemplateSpecBuilder;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.testutil.JsonUtil;
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

    resources = Stream.of(
        new SecretBuilder()
            .withData(ImmutableMap.of(getRandomString(), getRandomString()))
            .withNewMetadata()
            .withName("testSecret")
            .endMetadata().build(),
        new ConfigMapBuilder()
            .withData(ImmutableMap.of(getRandomString(), getRandomString()))
            .withNewMetadata().withName("testConfigMap")
            .endMetadata()
            .build(),
        new StatefulSetBuilder()
            .withNewMetadata().withNewName("testStatefulSet").endMetadata()
            .withNewSpec()
            .withTemplate(
                new PodTemplateSpecBuilder()
                    .withNewSpec()
                    .addNewContainer().withImage("randomimage")
                    .endContainer()
                    .endSpec()
                    .build())
            .withVolumeClaimTemplates(
                new PersistentVolumeClaimBuilder()
                    .withNewMetadata()
                    .withName("testVolumeClaim")
                    .endMetadata()
                    .withNewSpec()
                    .withAccessModes("ReadWriteOnce")
                    .endSpec()
                    .build())
            .endSpec().build(),
        new ServiceBuilder()
            .withNewMetadata()
            .withName("primary-" + PatroniUtil.READ_WRITE_SERVICE)
            .endMetadata()
            .build(),
        new ServiceBuilder()
            .withNewMetadata()
            .withName("replicas-" + PatroniUtil.READ_ONLY_SERVICE)
            .endMetadata()
            .build(),
        new PodBuilder()
            .withNewMetadata().withName("testpod")
            .endMetadata().build(),
        new CronJobBuilder()
            .withNewMetadata().withName("testcronjob")
            .endMetadata()
            .withNewSpec()
            .withJobTemplate(new JobTemplateSpecBuilder()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .addNewContainer()
                .withImage("randomimage")
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .build())
            .endSpec()
            .build(),
        new JobBuilder()
            .withNewMetadata()
            .withName("testjob")
            .endMetadata()
            .withNewSpec()
            .withNewTemplateLike(new PodTemplateSpecBuilder()
                .withNewSpec().addNewContainer()
                .withImage("randomimage")
                .endContainer().endSpec()
                .build())
            .endTemplate()
            .endSpec()
            .build()
    ).collect(Collectors.toList());
  }

  @Test
  void allResources_shouldBeAppliedToAllResources() {

    String randomAnnotationKey = getRandomString();
    String randomAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(randomAnnotationKey, randomAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

    resources.forEach(resource -> checkResourceAnnotations(resource,
        new Tuple2<>(randomAnnotationKey, randomAnnotationValue)));

  }

  @Test
  void services_shouldHaveServicesAnnotationsAndAllResourcesAnnotations() {
    String allResourceAnnotationKey = getRandomString();
    String allResourceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String serviceAnnotationKey = getRandomString();
    String serviceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(serviceAnnotationKey, serviceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
        .forEach(resource -> checkResourceAnnotations(resource,
            new Tuple2<>(allResourceAnnotationKey, allResourceAnnotationValue),
            new Tuple2<>(serviceAnnotationKey, serviceAnnotationValue)));

  }

  @Test
  void services_shouldNotHavePodAnnotations() {
    String allResourceAnnotationKey = getRandomString();
    String allResourceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String podAnnotationKey = getRandomString();
    String podAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setPods(ImmutableMap.of(podAnnotationKey, podAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Service"))
        .forEach(resource -> {
          assertFalse(resource.getMetadata().getAnnotations().containsKey(podAnnotationKey));
        });

  }

  @Test
  void primaryServices_shouldHavePrimaryServiceAnnotations() {
    String primaryAnnotationKey = getRandomString();
    String primaryAnnotationValue = getRandomString();

    defaultCluster.getSpec().getPostgresServices().getPrimary()
        .setAnnotations(ImmutableMap.of(primaryAnnotationKey, primaryAnnotationValue));

    String serviceAnnotationKey = getRandomString();
    String serviceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(serviceAnnotationKey, serviceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

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
    String replicaAnnotationKey = getRandomString();
    String replicaAnnotationValue = getRandomString();

    defaultCluster.getSpec().getPostgresServices().getReplicas()
        .setAnnotations(ImmutableMap.of(replicaAnnotationKey, replicaAnnotationValue));

    String serviceAnnotationKey = getRandomString();
    String serviceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(serviceAnnotationKey, serviceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

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
    String allResourceAnnotationKey = getRandomString();
    String allResourceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String podAnnotationKey = getRandomString();
    String podAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setPods(ImmutableMap.of(podAnnotationKey, podAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Pod"))
        .forEach(resource -> checkResourceAnnotations(resource,
            new Tuple2<>(allResourceAnnotationKey, allResourceAnnotationValue),
            new Tuple2<>(podAnnotationKey, podAnnotationValue)));

  }

  @Test
  void pods_shouldNotHaveServiceAnnotations() {
    String allResourceAnnotationKey = getRandomString();
    String allResourceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String serviceAnnotationKey = getRandomString();
    String serviceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setServices(ImmutableMap.of(serviceAnnotationKey, serviceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

    resources.stream()
        .filter(r -> r.getKind().equals("Pod"))
        .forEach(resource ->
            assertFalse(resource.getMetadata().getAnnotations().containsKey(serviceAnnotationKey)));

  }

  @Test
  void podsAnnotations_shouldBePresentInStatefulSetPodTemplates() {
    String allResourceAnnotationKey = getRandomString();
    String allResourceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    String podAnnotationKey = getRandomString();
    String podAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setPods(ImmutableMap.of(podAnnotationKey, podAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

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
  void allResourcesAnnotations_shouldBePresentInStatefulSetPersistenVolumeClaims() {
    String allResourceAnnotationKey = getRandomString();
    String allResourceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

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
  void allResourcesAnnotations_shouldBePresentInCronJobsPodTemplate() {
    String allResourceAnnotationKey = getRandomString();
    String allResourceAnnotationValue = getRandomString();

    defaultCluster.getSpec().getMetadata().getAnnotations()
        .setAllResources(ImmutableMap.of(allResourceAnnotationKey, allResourceAnnotationValue));

    annotationDecorator.decorate(defaultCluster, resources);

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

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElseGet(() -> fail("No annotations found for resource " + resource.getKind()));

    Arrays.asList(annotations).forEach(annotation -> {
      assertTrue(resourceAnnotation.containsKey(annotation.getFirst()));
      assertEquals(annotation.getSecond(), resourceAnnotation.get(annotation.getFirst()));
    });

  }

  @SafeVarargs
  private final void checkResourceAnnotations(PodTemplateSpec resource,
                                              Tuple2<String, String>... annotations) {

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElseGet(() -> fail("No annotations found for resource " + resource.toString()));

    Arrays.asList(annotations).forEach(annotation -> {
      assertTrue(resourceAnnotation.containsKey(annotation.getFirst()));
      assertEquals(annotation.getSecond(), resourceAnnotation.get(annotation.getFirst()));
    });

  }

  @SafeVarargs
  private final void checkResourceAnnotations(JobTemplateSpec resource,
                                              Tuple2<String, String>... annotations) {

    Map<String, String> resourceAnnotation = Optional.ofNullable(resource.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .orElseGet(() -> fail("No annotations found for resource " + resource.toString()));

    Arrays.asList(annotations).forEach(annotation -> {
      assertTrue(resourceAnnotation.containsKey(annotation.getFirst()));
      assertEquals(annotation.getSecond(), resourceAnnotation.get(annotation.getFirst()));
    });

  }

}