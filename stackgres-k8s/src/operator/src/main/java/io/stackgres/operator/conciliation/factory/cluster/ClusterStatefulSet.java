/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.TypedLocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ImmutableStorageConfig;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StorageConfig;
import io.stackgres.common.VolumeSnapshotUtil;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackupVolumeSnapshotStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresReplicationInitializationMode;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PodTemplateResult;
import io.stackgres.operator.conciliation.factory.VolumeDiscoverer;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class ClusterStatefulSet
    implements ResourceGenerator<StackGresClusterContext> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(ClusterStatefulSet.class);

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  private final PodTemplateFactoryDiscoverer<ClusterContainerContext>
      podTemplateSpecFactoryDiscoverer;
  private final VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer;

  @Inject
  public ClusterStatefulSet(
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      PodTemplateFactoryDiscoverer<ClusterContainerContext>
          podTemplateSpecFactoryDiscoverer,
      VolumeDiscoverer<StackGresClusterContext> volumeDiscoverer) {
    this.labelFactory = labelFactory;
    this.podTemplateSpecFactoryDiscoverer = podTemplateSpecFactoryDiscoverer;
    this.volumeDiscoverer = volumeDiscoverer;
  }

  public static String dataName(ClusterContext cluster) {
    return StackGresUtil.statefulSetDataPersistentVolumeClaimName(cluster);
  }

  public static String dataName(StackGresClusterContext clusterContext) {
    return StackGresUtil.statefulSetDataPersistentVolumeClaimName(clusterContext);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final ObjectMeta metadata = cluster.getMetadata();
    final String name = metadata.getName();
    final String namespace = metadata.getNamespace();

    final StackGresPodPersistentVolume persistentVolume = cluster
        .getSpec().getPods().getPersistentVolume();

    StorageConfig dataStorageConfig = ImmutableStorageConfig.builder()
        .size(persistentVolume.getSize())
        .storageClass(Optional.ofNullable(
            persistentVolume.getStorageClass())
            .orElse(null))
        .build();

    final Map<String, String> labels = labelFactory.clusterLabels(cluster);
    final Map<String, String> podLabels = labelFactory.statefulSetPodLabels(cluster);
    final Map<String, String> customPodLabels = context.clusterPodsCustomLabels();

    Map<String, VolumePair> availableVolumesPairs = volumeDiscoverer.discoverVolumes(context);

    final PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(dataStorageConfig.getVolumeResourceRequirements())
        .withStorageClassName(dataStorageConfig.getStorageClass())
        .withDataSource(context.getRestoreBackup()
            .filter(backpup -> context.getCurrentInstances() < 1)
            .or(context::getReplicationInitializationBackup)
            .map(StackGresBackup::getStatus)
            .filter(status -> Optional.of(status)
                .map(StackGresBackupStatus::getProcess)
                .map(StackGresBackupProcess::getStatus)
                .map(BackupStatus.COMPLETED.status()::equals)
                .orElse(false))
            .map(StackGresBackupStatus::getVolumeSnapshot)
            .map(StackGresBackupVolumeSnapshotStatus::getName)
            .map(volumeSnapshotName -> new TypedLocalObjectReferenceBuilder()
                .withApiGroup(VolumeSnapshotUtil.VOLUME_SNAPSHOT_GROUP)
                .withKind(VolumeSnapshotUtil.VOLUME_SNAPSHOT_KIND)
                .withName(volumeSnapshotName)
                .build())
            .orElse(null));

    Map<String, Volume> availableVolumes = availableVolumesPairs.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            vp -> vp.getValue().getVolume()
        ));

    final var installedExtensions = Optional
        .ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getToInstallPostgresExtensions)
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());
    var containerContext = ImmutableClusterContainerContext.builder()
        .clusterContext(context)
        .availableVolumes(availableVolumes)
        .dataVolumeName(dataName(context))
        .addAllInstalledExtensions(installedExtensions)
        .build();
    var podTemplateSpecFactory = podTemplateSpecFactoryDiscoverer
        .discoverPodSpecFactory(containerContext);

    PodTemplateResult podTemplateSpec = podTemplateSpecFactory.getPodTemplateSpec(containerContext);

    Integer instances = cluster.getSpec().getInstances();
    if (StackGresReplicationInitializationMode.FROM_NEWLY_CREATED_BACKUP.equals(
        context.getCluster().getSpec().getReplication().getInitializationModeOrDefault())
        && context.getReplicationInitializationBackup().isEmpty()
        && context.getReplicationInitializationBackupToCreate()
            .map(StackGresBackup::getStatus)
            .map(StackGresBackupStatus::getProcess)
            .map(StackGresBackupProcess::getStatus)
            .map(BackupStatus::fromStatus)
            .filter(BackupStatus.FAILED::equals)
            .isEmpty()
        && context.getCurrentInstances() < Optional.of(instances).orElse(0).intValue()) {
      instances = Math.max(1, context.getCurrentInstances());
      LOGGER.info("Skipping upscale while waiting for a fresh SGBackup to be created");
    }
    StatefulSet clusterStatefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withPodManagementPolicy(Optional
            .ofNullable(cluster.getSpec().getPods().getManagementPolicy())
            .orElse("OrderedReady"))
        .withReplicas(instances)
        .withSelector(new LabelSelectorBuilder()
            .addToMatchLabels(customPodLabels)
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
        .flatMap(Optional::stream)
        .toList();

    return Stream.concat(Stream.of(clusterStatefulSet), volumeDependencies.stream());
  }

}
