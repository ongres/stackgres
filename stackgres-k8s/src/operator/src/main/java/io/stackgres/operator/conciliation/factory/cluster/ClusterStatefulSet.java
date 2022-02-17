/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ImmutableStorageConfig;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StorageConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PodTemplateResult;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumePair;

@Singleton
@OperatorVersionBinder
public class ClusterStatefulSet
    implements ResourceGenerator<StackGresClusterContext> {

  public static final String GCS_CREDENTIALS_FILE_NAME = "gcs-credentials.json";
  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  private final PodTemplateFactoryDiscoverer<StackGresClusterContainerContext>
      podTemplateSpecFactoryDiscoverer;
  private final VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer;

  @Inject
  public ClusterStatefulSet(
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      PodTemplateFactoryDiscoverer<StackGresClusterContainerContext>
          podTemplateSpecFactoryDiscoverer,
      VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer) {
    this.labelFactory = labelFactory;
    this.podTemplateSpecFactoryDiscoverer = podTemplateSpecFactoryDiscoverer;
    this.volumeDiscoverer = volumeDiscoverer;
  }

  public static String dataName(ClusterContext cluster) {
    return StackGresUtil.statefulSetDataPersistentVolumeName(cluster);
  }

  public static String dataName(StackGresClusterContext clusterContext) {
    return StackGresUtil.statefulSetDataPersistentVolumeName(clusterContext);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {

    final StackGresCluster cluster = context.getSource();
    final ObjectMeta metadata = cluster.getMetadata();
    final String name = metadata.getName();
    final String namespace = metadata.getNamespace();

    final StackGresPodPersistentVolume persistentVolume = cluster
        .getSpec().getPod().getPersistentVolume();

    StorageConfig dataStorageConfig = ImmutableStorageConfig.builder()
        .size(persistentVolume.getSize())
        .storageClass(Optional.ofNullable(
            persistentVolume.getStorageClass())
            .orElse(null))
        .build();

    final Map<String, String> labels = labelFactory.clusterLabels(cluster);
    final Map<String, String> podLabels = labelFactory.statefulSetPodLabels(cluster);

    Map<String, VolumePair> availableVolumesPairs = volumeDiscoverer.discoverVolumes(context);

    final PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(dataStorageConfig.getResourceRequirements())
        .withStorageClassName(dataStorageConfig.getStorageClass());

    Map<String, Volume> availableVolumes = availableVolumesPairs.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            vp -> vp.getValue().getVolume()
        ));

    var containerContext = ImmutableStackGresClusterContainerContext.builder()
        .clusterContext(context)
        .availableVolumes(availableVolumes)
        .dataVolumeName(dataName(context))
        .build();
    var podTemplateSpecFactory = podTemplateSpecFactoryDiscoverer
        .discoverPodSpecFactory(containerContext);

    PodTemplateResult podTemplateSpec = podTemplateSpecFactory.getPodTemplateSpec(containerContext);

    StatefulSet clusterStatefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withPodManagementPolicy("OrderedReady")
        .withReplicas(cluster.getSpec().getInstances())
        .withSelector(new LabelSelectorBuilder()
            .addToMatchLabels(podLabels)
            .build())
        .withUpdateStrategy(new StatefulSetUpdateStrategyBuilder()
            .withType("OnDelete")
            .build())
        .withServiceName(name)
        .withTemplate(podTemplateSpec.getSpec())
        .withVolumeClaimTemplates(
            new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(dataName(context))
                .withLabels(labels)
                .endMetadata()
                .withSpec(volumeClaimSpec.build())
                .build()
        )
        .endSpec()
        .build();

    var volumeDependencies = podTemplateSpec.claimedVolumes().stream()
        .map(availableVolumesPairs::get)
        .filter(Objects::nonNull)
        .map(VolumePair::getSource)
        .filter(Optional::isPresent)
        .map(Optional::get);

    return Stream.concat(Stream.of(clusterStatefulSet), volumeDependencies);

  }

}
