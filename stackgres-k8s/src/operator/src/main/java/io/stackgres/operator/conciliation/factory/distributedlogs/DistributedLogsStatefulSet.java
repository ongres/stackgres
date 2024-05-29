/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyBuilder;
import io.stackgres.common.ImmutableStorageConfig;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StorageConfig;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsPersistentVolume;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.PodTemplateFactory;
import io.stackgres.operator.conciliation.factory.PodTemplateResult;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class DistributedLogsStatefulSet
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  private final PodTemplateFactory<DistributedLogsContainerContext> podTemplateSpecFactory;
  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;
  private final VolumeDiscoverer<StackGresDistributedLogsContext> volumeDiscoverer;

  @Inject
  public DistributedLogsStatefulSet(
      LabelFactoryForCluster<StackGresDistributedLogs> labelFactory,
      PodTemplateFactory<DistributedLogsContainerContext> podTemplateSpecFactory,
      VolumeDiscoverer<StackGresDistributedLogsContext> volumeDiscoverer) {
    this.labelFactory = labelFactory;
    this.podTemplateSpecFactory = podTemplateSpecFactory;
    this.volumeDiscoverer = volumeDiscoverer;
  }

  public static String dataName(StackGresDistributedLogs cluster) {
    return StackGresUtil.statefulSetDataPersistentVolumeClaimName(cluster);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {

    final StackGresDistributedLogs cluster = context.getSource();
    final ObjectMeta metadata = cluster.getMetadata();
    final String name = metadata.getName();
    final String namespace = metadata.getNamespace();

    final StackGresDistributedLogsPersistentVolume persistentVolume = cluster
        .getSpec().getPersistentVolume();

    StorageConfig dataStorageConfig = ImmutableStorageConfig.builder()
        .size(persistentVolume.getSize())
        .storageClass(Optional.ofNullable(
            persistentVolume.getStorageClass())
            .orElse(null))
        .build();

    final PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(dataStorageConfig.getVolumeResourceRequirements())
        .withStorageClassName(dataStorageConfig.getStorageClass());

    final Map<String, String> labels = labelFactory.clusterLabels(cluster);
    final Map<String, String> podLabels = labelFactory.statefulSetPodLabels(cluster);

    Map<String, VolumePair> availableVolumesPairs = volumeDiscoverer.discoverVolumes(context);

    Map<String, Volume> availableVolumes = availableVolumesPairs.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            vp -> vp.getValue().getVolume()
        ));

    final PodTemplateResult buildPodTemplate = podTemplateSpecFactory
        .getPodTemplateSpec(ImmutableDistributedLogsContainerContext.builder()
            .distributedLogsContext(context)
            .availableVolumes(availableVolumes)
            .dataVolumeName(dataName(cluster))
            .build());

    StatefulSet clusterStatefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withReplicas(1)
        .withSelector(new LabelSelectorBuilder()
            .addToMatchLabels(podLabels)
            .build())
        .withUpdateStrategy(new StatefulSetUpdateStrategyBuilder()
            .withType("OnDelete")
            .build())
        .withServiceName(name)
        .withTemplate(buildPodTemplate.getSpec())
        .withVolumeClaimTemplates(Stream.of(
            Stream.of(new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(dataName(cluster))
                .withLabels(labels)
                .endMetadata()
                .withSpec(volumeClaimSpec.build())
                .build()))
            .flatMap(s -> s)
            .toArray(PersistentVolumeClaim[]::new))
        .endSpec()
        .build();

    var volumeDependencies = buildPodTemplate.claimedVolumes().stream()
        .map(availableVolumesPairs::get)
        .filter(Objects::nonNull)
        .map(VolumePair::getSource)
        .flatMap(Optional::stream);

    return Stream.concat(Stream.of(clusterStatefulSet), volumeDependencies);
  }

}
