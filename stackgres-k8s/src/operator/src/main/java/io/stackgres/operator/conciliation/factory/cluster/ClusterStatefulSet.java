/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ImmutableStorageConfig;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StorageConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ResourceFactory;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class ClusterStatefulSet
    implements ResourceGenerator<StackGresClusterContext> {

  public static final String GCS_CREDENTIALS_FILE_NAME = "gcs-credentials.json";
  private final LabelFactory<StackGresCluster> labelFactory;

  private final ResourceFactory<StackGresClusterContext, PodTemplateSpec> podSpecFactory;

  @Inject
  public ClusterStatefulSet(
      LabelFactory<StackGresCluster> labelFactory,
      ResourceFactory<StackGresClusterContext, PodTemplateSpec> podSpecFactory) {
    this.labelFactory = labelFactory;
    this.podSpecFactory = podSpecFactory;
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

    final PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(dataStorageConfig.getResourceRequirements())
        .withStorageClassName(dataStorageConfig.getStorageClass());

    final Map<String, String> labels = labelFactory.clusterLabels(cluster);
    final Map<String, String> podLabels = labelFactory.statefulSetPodLabels(cluster);

    StatefulSet clusterStatefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
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
        .withTemplate(podSpecFactory.createResource(context))
        .withVolumeClaimTemplates(Stream.of(
            Stream.of(new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(dataName(context))
                .withLabels(labels)
                .endMetadata()
                .withSpec(volumeClaimSpec.build())
                .build()))
            .flatMap(s -> s)
            .toArray(PersistentVolumeClaim[]::new))
        .endSpec()
        .build();

    return Stream.of(clusterStatefulSet);
  }

}
