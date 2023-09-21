/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.cluster.ImmutableStackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.testutil.GeneratorTestUtil;
import org.opentest4j.AssertionFailedError;

public class GeneratedResourceMatcher {
  private final StackGresCluster cluster;
  private final ResourceGenerationDiscoverer<StackGresClusterContext> resourceGenerator;
  private final String clusterName;
  private final String clusterNamespace;

  private StackGresPostgresConfig stackGresPostgresConfig;
  private StackGresProfile stackGresProfile;
  private Secret databaseSecret;

  private GeneratedResourceMatcher(
      StackGresCluster cluster,
      ResourceGenerationDiscoverer<StackGresClusterContext> resourceGenerator) {
    this.cluster = cluster;
    this.resourceGenerator = resourceGenerator;
    this.clusterName = cluster.getMetadata().getName();
    this.clusterNamespace = cluster.getMetadata().getNamespace();
  }

  public static GeneratedResourceMatcher givenACluster(
      StackGresCluster cluster,
      ResourceGenerationDiscoverer<StackGresClusterContext> resourceGenerator) {
    return new GeneratedResourceMatcher(cluster, resourceGenerator);
  }

  protected List<HasMetadata> getResources(StackGresClusterContext context) {
    return resourceGenerator.generateResources(context);
  }

  protected List<HasMetadata> getResources() {
    var context = buildContext();
    return getResources(context);
  }

  private StackGresClusterContext buildContext() {
    return ImmutableStackGresClusterContext.builder()
        .source(cluster)
        .profile(stackGresProfile)
        .postgresConfig(stackGresPostgresConfig)
        .databaseSecret(Optional.ofNullable(databaseSecret))
        .prometheus(new Prometheus(false, null))
        .build();
  }

  private <T> T generateResource(HasMetadata expectedResource, Class<T> clazz) {
    final List<HasMetadata> resourcesOfTheSameKind = getResources().stream()
        .filter(r -> r.getKind().equals(expectedResource.getKind()))
        .collect(Collectors.toList());

    return resourcesOfTheSameKind.stream()
        .filter(resource -> resource.getMetadata().getName()
            .equals(expectedResource.getMetadata().getName()))
        .filter(resource -> resource.getMetadata().getNamespace()
            .equals(expectedResource.getMetadata().getNamespace()))
        .findAny()
        .map(clazz::cast)
        .orElseThrow(() -> {
          List<String> sameKindResourceNames = resourcesOfTheSameKind.stream()
              .map(HasMetadata::getMetadata)
              .map(ObjectMeta::getName)
              .collect(Collectors.toList());
          return new AssertionFailedError(
              "Resource not generated, other generated resources of the same kind are "
                  + sameKindResourceNames);
        });

  }

  public GeneratedResourceMatcher and(Consumer<StackGresCluster> clusterConsumer) {
    clusterConsumer.accept(cluster);
    return this;
  }

  public GeneratedResourceMatcher andPostgresConfig(StackGresPostgresConfig postgresConfig) {
    this.stackGresPostgresConfig = postgresConfig;
    this.cluster.getSpec().getConfigurations()
        .setSgPostgresConfig(postgresConfig.getMetadata().getName());
    return this;
  }

  public GeneratedResourceMatcher andDatabaseCredentials(Secret databaseCredentials) {
    this.databaseSecret = databaseCredentials;
    return this;
  }

  public GeneratedResourceMatcher andNumberOfInstances(int instances) {
    cluster.getSpec().setInstances(instances);
    return this;
  }

  public GeneratedResourceMatcher andStorageSize(String storageSize) {
    if (cluster.getSpec().getPods() == null) {
      cluster.getSpec().setPods(new StackGresClusterPods());
    }
    if (cluster.getSpec().getPods().getPersistentVolume() == null) {
      cluster.getSpec().getPods().setPersistentVolume(new StackGresPodPersistentVolume());
    }
    cluster.getSpec().getPods().getPersistentVolume().setSize(storageSize);
    return this;
  }

  public GeneratedResourceMatcher andInstanceProfile(String cpu, String memory) {
    var instanceProfile = new StackGresProfile();
    instanceProfile.setMetadata(new ObjectMeta());
    instanceProfile.getMetadata().setName(clusterName);
    instanceProfile.getMetadata().setNamespace(clusterNamespace);
    instanceProfile.setSpec(new StackGresProfileSpec());
    instanceProfile.getSpec().setCpu(cpu);
    instanceProfile.getSpec().setMemory(memory);
    cluster.getSpec().setSgInstanceProfile(clusterNamespace);
    stackGresProfile = instanceProfile;
    return this;
  }

  public GeneratedResourceMatcher andInstanceProfile(StackGresProfile profile) {
    this.stackGresProfile = profile;
    cluster.getSpec().setSgInstanceProfile(profile.getMetadata().getName());
    return this;
  }

  private void ensureAnnotationsConfiguration() {
    if (cluster.getSpec().getMetadata() == null) {
      cluster.getSpec().setMetadata(new StackGresClusterSpecMetadata());
    }
    if (cluster.getSpec().getMetadata().getAnnotations() == null) {
      cluster.getSpec().getMetadata().setAnnotations(new StackGresClusterSpecAnnotations());
    }
  }

  public GeneratedResourceMatcher andAllResourceAnnotations(Map<String, String> annotations) {
    ensureAnnotationsConfiguration();
    cluster.getSpec().getMetadata()
        .getAnnotations().setAllResources(annotations);
    return this;
  }

  public GeneratedResourceMatcher andPodAnnotations(Map<String, String> annotations) {
    ensureAnnotationsConfiguration();
    cluster.getSpec().getMetadata().getAnnotations().setClusterPods(annotations);
    return this;
  }

  public GeneratedResourceMatcher andServiceAnnotations(Map<String, String> annotations) {
    ensureAnnotationsConfiguration();
    cluster.getSpec().getMetadata()
        .getAnnotations().setServices(annotations);
    return this;
  }

  public GeneratedResourceMatcher andPrimaryServiceAnnotations(Map<String, String> annotations) {
    ensureAnnotationsConfiguration();
    cluster.getSpec().getMetadata()
        .getAnnotations().setPrimaryService(annotations);
    return this;
  }

  public GeneratedResourceMatcher andReplicasServiceAnnotations(Map<String, String> annotations) {
    ensureAnnotationsConfiguration();
    cluster.getSpec().getMetadata()
        .getAnnotations().setReplicasService(annotations);
    return this;
  }

  public void generatedResourceShouldBeEqualTo(Service expectedResource) {
    var actualResource = generateResource(expectedResource, Service.class);
    GeneratorTestUtil.assertResourceEquals(expectedResource, actualResource);
  }

  public void generatedResourceShouldBeEqualTo(Role expectedResource) {
    var actualResource = generateResource(expectedResource, Role.class);
    GeneratorTestUtil.assertResourceEquals(expectedResource, actualResource);
  }

  public void generatedResourceShouldBeEqualTo(Secret expectedResource) {
    var actualResource = generateResource(expectedResource, Secret.class);
    GeneratorTestUtil.assertResourceEquals(expectedResource, actualResource);
  }

  public void generatedResourceShouldBeEqualTo(ConfigMap expectedResource) {
    var actualResource = generateResource(expectedResource, ConfigMap.class);
    GeneratorTestUtil.assertResourceEquals(expectedResource, actualResource);
  }

  public void generatedResourceShouldBeEqualTo(StatefulSet expectedResource) {
    var actualResource = generateResource(expectedResource, StatefulSet.class);
    GeneratorTestUtil.assertResourceEquals(expectedResource, actualResource);
  }

  public void generatedResourceShouldBeEqualTo(HasMetadata expectedResource) {
    var actualResource = generateResource(expectedResource, HasMetadata.class);
    GeneratorTestUtil.assertResourceEquals(expectedResource, actualResource);
  }

}
