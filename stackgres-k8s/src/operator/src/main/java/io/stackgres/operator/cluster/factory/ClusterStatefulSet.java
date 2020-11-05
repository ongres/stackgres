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
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyBuilder;
import io.stackgres.common.ImmutableStorageConfig;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StorageConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodScheduling;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresPodSecurityContext;
import io.stackgres.operator.patroni.factory.Patroni;
import io.stackgres.operator.patroni.factory.PatroniRole;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ClusterStatefulSet implements StackGresClusterResourceStreamFactory {

  public static final String GCS_CREDENTIALS_FILE_NAME = "gcs-credentials.json";

  private final Patroni patroni;
  private final StackGresPodSecurityContext clusterPodSecurityContext;
  private final ClusterStatefulSetInitContainers initContainerFactory;
  private final ClusterStatefulSetVolumes volumesFactory;
  private final TemplatesConfigMap templatesConfigMap;

  private final LabelFactoryDelegator factoryDelegator;

  @Inject
  public ClusterStatefulSet(Patroni patroni, StackGresPodSecurityContext clusterPodSecurityContext,
      ClusterStatefulSetInitContainers initContainerFactory,
      ClusterStatefulSetVolumes volumesFactory,
      TemplatesConfigMap templatesConfigMap, LabelFactoryDelegator factoryDelegator) {
    super();
    this.patroni = patroni;
    this.clusterPodSecurityContext = clusterPodSecurityContext;
    this.initContainerFactory = initContainerFactory;
    this.volumesFactory = volumesFactory;
    this.templatesConfigMap = templatesConfigMap;
    this.factoryDelegator = factoryDelegator;
  }

  public static String dataName(StackGresClusterContext clusterContext) {
    return StackGresUtil.statefulSetDataPersistentVolumeName(clusterContext.getCluster());
  }

  public static String backupName(StackGresClusterContext clusterContext) {
    return StackGresUtil.statefulSetBackupPersistentVolumeName(clusterContext.getCluster());
  }

  /**
   * Create a new StatefulSet based on the StackGresCluster definition.
   */
  @Override
  public Stream<HasMetadata> streamResources(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();

    StorageConfig dataStorageConfig = ImmutableStorageConfig.builder()
        .size(cluster.getSpec().getPod().getPersistentVolume().getVolumeSize())
        .storageClass(Optional.ofNullable(
            cluster.getSpec().getPod().getPersistentVolume().getStorageClass())
            .orElse(null))
        .build();

    final PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(dataStorageConfig.getResourceRequirements())
        .withStorageClassName(dataStorageConfig.getStorageClass());

    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(context);
    final Map<String, String> labels = labelFactory.clusterLabels(cluster);
    final Map<String, String> podLabels = labelFactory.statefulSetPodLabels(cluster);
    final Map<String, String> customPodLabels = context.posCustomLabels();
    StatefulSet clusterStatefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .withNewSpec()
        .withReplicas(cluster.getSpec().getInstances())
        .withSelector(new LabelSelectorBuilder()
            .addToMatchLabels(podLabels)
            .build())
        .withUpdateStrategy(new StatefulSetUpdateStrategyBuilder()
            .withType("OnDelete")
            .build())
        .withServiceName(name)
        .withTemplate(new PodTemplateSpecBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(customPodLabels)
                .addToLabels(podLabels)
                .addToAnnotations(StackGresContext.VERSION_KEY,
                    StackGresProperty.OPERATOR_VERSION.getString())
                .build())
            .withNewSpec()
            .withAffinity(Optional.of(new AffinityBuilder()
                .withPodAntiAffinity(new PodAntiAffinityBuilder()
                    .addAllToRequiredDuringSchedulingIgnoredDuringExecution(ImmutableList.of(
                        new PodAffinityTermBuilder()
                            .withLabelSelector(new LabelSelectorBuilder()
                                .withMatchExpressions(new LabelSelectorRequirementBuilder()
                                        .withKey(StackGresContext.APP_KEY)
                                        .withOperator("In")
                                        .withValues(labelFactory.getLabelMapper().appName())
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
                    cluster.getSpec().getNonProduction())
                    .map(StackGresClusterNonProduction::getDisableClusterPodAntiAffinity)
                    .map(disableClusterPodAntiAffinity -> !disableClusterPodAntiAffinity)
                    .orElse(true))
                .orElse(null))
            .withNodeSelector(Optional.ofNullable(cluster.getSpec())
                .map(StackGresClusterSpec::getPod)
                .map(StackGresClusterPod::getScheduling)
                .map(StackGresPodScheduling::getNodeSelector)
                .orElse(null))
            .withTolerations(Optional.ofNullable(cluster.getSpec())
                .map(StackGresClusterSpec::getPod)
                .map(StackGresClusterPod::getScheduling)
                .map(StackGresPodScheduling::getTolerations)
                .map(tolerations -> Seq.seq(tolerations)
                    .map(TolerationBuilder::new)
                    .map(TolerationBuilder::build)
                    .toList())
                .orElse(null))
            .withShareProcessNamespace(Boolean.TRUE)
            .withServiceAccountName(PatroniRole.roleName(context))
            .withSecurityContext(clusterPodSecurityContext.createResource(context))
            .withVolumes(volumesFactory.listResources(context))
            .withTerminationGracePeriodSeconds(60L)
            .addToContainers(patroni.getContainer(context))
            .addAllToVolumes(patroni.getVolumes(context))
            .withInitContainers(initContainerFactory.listResources(context))
            .addAllToContainers(context.getSidecars().stream()
                .map(sidecarEntry -> sidecarEntry.getSidecar().getContainer(context))
                .collect(ImmutableList.toImmutableList()))
            .addAllToInitContainers(context.getSidecars().stream()
                .flatMap(sidecarEntry -> sidecarEntry.getSidecar().getInitContainers(context))
                .collect(ImmutableList.toImmutableList()))
            .addAllToVolumes(context.getSidecars().stream()
                .flatMap(sidecarEntry -> sidecarEntry.getSidecar().getVolumes(context).stream())
                .collect(ImmutableList.toImmutableList()))
            .endSpec()
            .build())
        .withVolumeClaimTemplates(Stream.of(
            Stream.of(new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(dataName(context))
                .withLabels(labels)
                .withOwnerReferences(context.getOwnerReferences())
                .endMetadata()
                .withSpec(volumeClaimSpec.build())
                .build()))
            .flatMap(s -> s)
            .toArray(PersistentVolumeClaim[]::new))
        .endSpec()
        .build();

    return Seq.<HasMetadata>empty()
        .append(templatesConfigMap.streamResources(context))
        .append(patroni.streamResources(context))
        .append(context.getSidecars().stream()
            .flatMap(sidecarEntry -> sidecarEntry.getSidecar().streamResources(context)))
        .append(Seq.seq(context.getExistingResources())
            .map(Tuple2::v1)
            .filter(Pod.class::isInstance)
            .map(HasMetadata::getMetadata)
            .filter(existingPodMetadata -> Objects.equals(
                existingPodMetadata.getLabels().get(StackGresContext.CLUSTER_KEY),
                StackGresContext.RIGHT_VALUE))
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
