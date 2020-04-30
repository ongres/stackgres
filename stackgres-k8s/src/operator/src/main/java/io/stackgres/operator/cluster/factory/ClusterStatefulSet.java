/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.PodAntiAffinityBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.configuration.ImmutableStorageConfig;
import io.stackgres.operator.configuration.StorageConfig;
import io.stackgres.operator.patroni.factory.Patroni;
import io.stackgres.operator.patroni.factory.PatroniRole;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterStatefulSet implements StackGresClusterResourceStreamFactory {

  public static final String DATA_SUFFIX = "-data";
  public static final String BACKUP_SUFFIX = "-backup";

  public static final String GCS_CREDENTIALS_FILE_NAME = "gcs-credentials.json";

  private Patroni patroni;
  private ClusterStatefulSetInitContainers initContainerFactory;
  private ClusterStatefulSetVolumes volumesFactory;

  @Inject
  public void setPatroni(Patroni patroni) {
    this.patroni = patroni;
  }

  @Inject
  public void setInitContainerFactory(ClusterStatefulSetInitContainers initContainerFactory) {
    this.initContainerFactory = initContainerFactory;
  }

  @Inject
  public void setVolumesFactory(ClusterStatefulSetVolumes volumesFactory) {
    this.volumesFactory = volumesFactory;
  }

  public static String dataName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + ClusterStatefulSet.DATA_SUFFIX);
  }

  public static String backupName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + ClusterStatefulSet.BACKUP_SUFFIX);
  }

  /**
   * Create a new StatefulSet based on the StackGresCluster definition.
   */
  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    StackGresClusterContext clusterContext = context.getClusterContext();

    final String name = clusterContext.getCluster().getMetadata().getName();
    final String namespace = clusterContext.getCluster().getMetadata().getNamespace();

    StorageConfig dataStorageConfig = ImmutableStorageConfig.builder()
        .size(clusterContext.getCluster().getSpec().getPod().getPersistentVolume().getVolumeSize())
        .storageClass(Optional.ofNullable(
            clusterContext.getCluster().getSpec().getPod().getPersistentVolume().getStorageClass())
            .orElse(null))
        .build();

    final PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(dataStorageConfig.getResourceRequirements())
        .withStorageClassName(dataStorageConfig.getStorageClass());

    final Map<String, String> labels = clusterContext.clusterLabels();
    final Map<String, String> podLabels = clusterContext.statefulSetPodLabels();

    StatefulSet clusterStatefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(context.getClusterContext().ownerReference())
        .endMetadata()
        .withNewSpec()
        .withReplicas(clusterContext.getCluster().getSpec().getInstances())
        .withSelector(new LabelSelectorBuilder()
            .addToMatchLabels(podLabels)
            .build())
        .withUpdateStrategy(new StatefulSetUpdateStrategyBuilder()
            .withType("OnDelete")
            .build())
        .withServiceName(name)
        .withTemplate(new PodTemplateSpecBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(podLabels)
                .build())
            .withNewSpec()
            .withAffinity(Optional.of(new AffinityBuilder()
                .withPodAntiAffinity(new PodAntiAffinityBuilder()
                    .addAllToRequiredDuringSchedulingIgnoredDuringExecution(ImmutableList.of(
                        new PodAffinityTermBuilder()
                            .withLabelSelector(new LabelSelectorBuilder()
                                .withMatchExpressions(new LabelSelectorRequirementBuilder()
                                        .withKey(StackGresUtil.APP_KEY)
                                        .withOperator("In")
                                        .withValues(clusterContext.appName())
                                        .build(),
                                    new LabelSelectorRequirementBuilder()
                                        .withKey("cluster")
                                        .withOperator("In")
                                        .withValues("true")
                                        .build())
                                .build())
                            .withTopologyKey("kubernetes.io/hostname")
                            .build()))
                    .build())
                .build())
                .filter(affinity -> Optional.ofNullable(
                    clusterContext.getCluster().getSpec().getNonProduction())
                    .map(nonProduction -> nonProduction.getDisableClusterPodAntiAffinity())
                    .map(disableClusterPodAntiAffinity -> !disableClusterPodAntiAffinity)
                    .orElse(true))
                .orElse(null))
            .withShareProcessNamespace(Boolean.TRUE)
            .withServiceAccountName(PatroniRole.roleName(clusterContext))
            .withVolumes(volumesFactory.listResources(clusterContext))
            .withTerminationGracePeriodSeconds(60L)
            .addToContainers(patroni.getContainer(context))
            .addAllToVolumes(patroni.getVolumes(context))
            .withInitContainers(initContainerFactory.listResources(clusterContext))
            .addAllToContainers(clusterContext.getSidecars().stream()
                .map(sidecarEntry -> sidecarEntry.getSidecar().getContainer(context))
                .collect(ImmutableList.toImmutableList()))
            .addAllToVolumes(clusterContext.getSidecars().stream()
                .flatMap(sidecarEntry -> sidecarEntry.getSidecar().getVolumes(context).stream())
                .collect(ImmutableList.toImmutableList()))
            .endSpec()
            .build())
        .withVolumeClaimTemplates(Stream.of(
            Stream.of(new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(dataName(clusterContext))
                .withLabels(labels)
                .withOwnerReferences(context.getClusterContext().ownerReference())
                .endMetadata()
                .withSpec(volumeClaimSpec.build())
                .build()))
            .flatMap(s -> s)
            .toArray(PersistentVolumeClaim[]::new))
        .endSpec()
        .build();

    return Seq.<HasMetadata>empty()
        .append(patroni.streamResources(context))
        .append(clusterContext.getSidecars().stream()
            .flatMap(sidecarEntry -> sidecarEntry.getSidecar().streamResources(context)))
        .append(Seq.seq(context.getClusterContext().getExistingResources())
            .map(Tuple2::v1)
            .filter(existingResource -> existingResource instanceof Pod)
            .map(HasMetadata::getMetadata)
            .filter(existingPodMetadata -> Objects.equals(
                existingPodMetadata.getLabels().get(StackGresUtil.CLUSTER_KEY),
                StackGresUtil.RIGHT_VALUE))
            .map(existingPodMetadata -> new PodBuilder()
                .withNewMetadata()
                .withNamespace(existingPodMetadata.getNamespace())
                .withName(existingPodMetadata.getName())
                .withLabels(podLabels)
                .endMetadata()
                .build()))
        .append(clusterStatefulSet);
  }

}
