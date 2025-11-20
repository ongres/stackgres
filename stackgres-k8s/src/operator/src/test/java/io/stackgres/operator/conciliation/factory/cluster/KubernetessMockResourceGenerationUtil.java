/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.StringUtil.generateRandom;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.operatorframework.resource.ResourceUtil;

public class KubernetessMockResourceGenerationUtil {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  public static List<HasMetadata> buildResources(String name, String namespace) {
    StackGresCluster cluster = new StackGresClusterBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withUid(generateRandom())
        .endMetadata()
        .withNewSpec()
        .withNewPostgres()
        .withVersion(POSTGRES_VERSION)
        .endPostgres()
        .endSpec()
        .withNewStatus()
        .withPostgresVersion(POSTGRES_VERSION)
        .endStatus()
        .build();
    return buildResources(cluster);
  }

  public static List<HasMetadata> buildResources(StackGresCluster cluster) {
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    ClusterLabelFactory labelFactory = new ClusterLabelFactory(new ClusterLabelMapper());
    return Stream.of(
        new SecretBuilder()
            .withData(ImmutableMap.of(generateRandom(), generateRandom()))
            .withNewMetadata()
            .withName(name)
            .withNamespace(namespace)
            .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
            .endMetadata().build(),
        new ConfigMapBuilder()
            .withData(ImmutableMap.of(generateRandom(), generateRandom()))
            .withNewMetadata()
            .withName(name)
            .withNamespace(namespace)
            .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
            .endMetadata()
            .build(),
        new ConfigMapBuilder()
            .withData(ImmutableMap.of(generateRandom(), generateRandom()))
            .withNewMetadata().withName(name + "-templates")
            .withNamespace(namespace)
            .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
            .endMetadata()
            .build(),
        new StatefulSetBuilder()
            .withNewMetadata()
            .withName(name)
            .withNamespace(namespace)
            .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
            .endMetadata()
            .withNewSpec()
            .withReplicas(2)
            .withTemplate(
                new PodTemplateSpecBuilder()
                    .withNewMetadata()
                    .endMetadata()
                    .withNewSpec()
                    .addNewContainer()
                    .withName(StackGresContainer.PATRONI.getName())
                    .withImage(generateRandom())
                    .endContainer()
                    .addNewContainer()
                    .withName(generateRandom())
                    .withImage(generateRandom())
                    .endContainer()
                    .addNewInitContainer()
                    .withName(generateRandom())
                    .withImage(generateRandom())
                    .endInitContainer()
                    .endSpec()
                    .build())
            .withVolumeClaimTemplates(
                new PersistentVolumeClaimBuilder()
                    .withNewMetadata()
                    .withName(generateRandom())
                    .endMetadata()
                    .withNewSpec()
                    .withAccessModes("ReadWriteOnce")
                    .endSpec()
                    .build())
            .endSpec().build(),
        new ServiceBuilder()
            .withNewMetadata()
            .withName(name + PatroniUtil.DEPRECATED_READ_WRITE_SERVICE)
            .withLabels(ImmutableMap.of(generateRandom(), generateRandom()))
            .withNamespace(namespace)
            .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
            .endMetadata()
            .build(),
        new ServiceBuilder()
            .withNewMetadata()
            .withLabels(ImmutableMap.of(generateRandom(), generateRandom()))
            .withName(name + PatroniUtil.READ_ONLY_SERVICE)
            .withNamespace(namespace)
            .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
            .endMetadata()
            .build(),
        new EndpointsBuilder()
            .withNewMetadata()
            .withName(name + PatroniUtil.DEPRECATED_READ_WRITE_SERVICE)
            .withLabels(ImmutableMap.of(generateRandom(), generateRandom()))
            .withNamespace(namespace)
            .endMetadata()
            .build(),
        new EndpointsBuilder()
            .withNewMetadata()
            .withLabels(ImmutableMap.of(generateRandom(), generateRandom()))
            .withName(name + PatroniUtil.READ_ONLY_SERVICE)
            .withNamespace(namespace)
            .endMetadata()
            .build(),
        new PodBuilder()
            .withNewMetadata().withName(name + "-0").withNamespace(namespace)
            .withLabels(labelFactory.clusterReplicaLabels(cluster))
            .withOwnerReferences(List.of(ResourceUtil.getOwnerReference(cluster)))
            .endMetadata()
            .withSpec(new PodSpecBuilder()
                .addNewContainer()
                .withImage(generateRandom())
                .endContainer()
                .build())
            .build(),
        new PodBuilder()
            .withNewMetadata().withName(name + "-1").withNamespace(namespace)
            .withLabels(labelFactory.clusterPrimaryLabels(cluster))
            .withOwnerReferences(List.of(ResourceUtil.getOwnerReference(cluster)))
            .endMetadata()
            .withSpec(new PodSpecBuilder()
                .addNewContainer()
                .withImage(generateRandom())
                .endContainer()
                .build())
            .build(),
        new CronJobBuilder()
            .withNewMetadata()
            .withName(name + "-backup")
            .withNamespace(namespace)
            .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
            .endMetadata()
            .withNewSpec()
            .withNewJobTemplate()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer()
            .withName(generateRandom())
            .withImage(generateRandom())
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .endJobTemplate()
            .endSpec()
            .build(),
        new JobBuilder()
            .withNewMetadata()
            .withName(name)
            .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
            .withNewTemplateLike(new PodTemplateSpecBuilder()
                .withNewSpec()
                .addNewContainer()
                .withName(generateRandom())
                .withImage(generateRandom())
                .endContainer().endSpec()
                .build())
            .endTemplate()
            .endSpec()
            .build()
    ).collect(Collectors.toList());
  }

}
