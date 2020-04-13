/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.Optional;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresRestoreContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;

import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClusterStatefulSetVolumeConfigTest {

  @Test
  public void volumesForCluster() {
    ImmutableList<Volume> expectedVolumes = Seq.of(
        new VolumeBuilder()
        .withName("socket")
        .withNewEmptyDir()
        .withMedium("Memory")
        .endEmptyDir()
        .build(),
        new VolumeBuilder()
        .withName("local-bin")
        .withNewEmptyDir()
        .withMedium("Memory")
        .endEmptyDir()
        .build(),
        new VolumeBuilder()
        .withName("patroni-config")
        .withNewConfigMap()
        .withName("test")
        .withDefaultMode(444)
        .endConfigMap()
        .build(),
        new VolumeBuilder()
        .withName("backup-config")
        .withNewConfigMap()
        .withName("test-backup")
        .withDefaultMode(444)
        .endConfigMap()
        .build(),
        new VolumeBuilder()
        .withName("backup-secret")
        .withNewSecret()
        .withSecretName("test-backup")
        .withDefaultMode(444)
        .endSecret()
        .build())
        .sorted((left, right) -> left.getName().compareTo(right.getName()))
        .collect(ImmutableList.toImmutableList());
    StackGresClusterContext context = createStackGresClusterContext();
    ImmutableList<Volume> actualVolumes = ClusterStatefulSetVolumeConfig.volumes(context)
        .sorted((left, right) -> left.getName().compareTo(right.getName()))
        .collect(ImmutableList.toImmutableList());
    Assertions.assertEquals(expectedVolumes, actualVolumes);
  }

  @Test
  public void volumeMountsForCluster() {
    ImmutableList<String> expectedVolumeMounts = Seq.of(
        "test-data",
        "socket",
        "local-bin",
        "patroni-config",
        "backup-config",
        "backup-secret")
        .sorted()
        .collect(ImmutableList.toImmutableList());
    StackGresClusterContext context = createStackGresClusterContext();
    ImmutableList<String> actualVolumeMounts = ClusterStatefulSetVolumeConfig.volumeMounts(context)
        .map(volume -> volume.getName())
        .sorted()
        .collect(ImmutableList.toImmutableList());
    Assertions.assertEquals(expectedVolumeMounts, actualVolumeMounts);
  }

  @Test
  public void volumesForClusterWithRestore() {
    ImmutableList<Volume> expectedVolumes = Seq.of(
        new VolumeBuilder()
        .withName("socket")
        .withNewEmptyDir()
        .withMedium("Memory")
        .endEmptyDir()
        .build(),
        new VolumeBuilder()
        .withName("local-bin")
        .withNewEmptyDir()
        .withMedium("Memory")
        .endEmptyDir()
        .build(),
        new VolumeBuilder()
        .withName("patroni-config")
        .withNewConfigMap()
        .withName("test")
        .withDefaultMode(444)
        .endConfigMap()
        .build(),
        new VolumeBuilder()
        .withName("backup-config")
        .withNewConfigMap()
        .withName("test-backup")
        .withDefaultMode(444)
        .endConfigMap()
        .build(),
        new VolumeBuilder()
        .withName("backup-secret")
        .withNewSecret()
        .withSecretName("test-backup")
        .withDefaultMode(444)
        .endSecret()
        .build(),
        new VolumeBuilder()
        .withName("restore-config")
        .withNewConfigMap()
        .withName("test-restore")
        .withDefaultMode(444)
        .endConfigMap()
        .build(),
        new VolumeBuilder()
        .withName("restore-secret")
        .withNewSecret()
        .withSecretName("test-restore")
        .withDefaultMode(444)
        .endSecret()
        .build(),
        new VolumeBuilder()
        .withName("restore-entrypoint")
        .withNewEmptyDir()
        .withMedium("Memory")
        .endEmptyDir()
        .build())
        .sorted((left, right) -> left.getName().compareTo(right.getName()))
        .collect(ImmutableList.toImmutableList());
    StackGresClusterContext context = createStackGresClusterContextWithRestore();
    ImmutableList<Volume> actualVolumes = ClusterStatefulSetVolumeConfig.volumes(context)
        .sorted((left, right) -> left.getName().compareTo(right.getName()))
        .collect(ImmutableList.toImmutableList());
    Assertions.assertEquals(expectedVolumes, actualVolumes);
  }

  @Test
  public void volumeMountsForClusterWithRestore() {
    ImmutableList<String> expectedVolumeMounts = Seq.of(
        "test-data",
        "socket",
        "local-bin",
        "patroni-config",
        "backup-config",
        "backup-secret",
        "restore-config",
        "restore-secret",
        "restore-entrypoint")
        .sorted()
        .collect(ImmutableList.toImmutableList());
    StackGresClusterContext context = createStackGresClusterContextWithRestore();
    ImmutableList<String> actualVolumeMounts = ClusterStatefulSetVolumeConfig.volumeMounts(context)
        .map(volume -> volume.getName())
        .sorted()
        .collect(ImmutableList.toImmutableList());
    Assertions.assertEquals(expectedVolumeMounts, actualVolumeMounts);
  }

  private StackGresClusterContext createStackGresClusterContext() {
    return StackGresClusterContext.builder()
        .withCluster(Unchecked.supplier(() -> {
          StackGresCluster cluster = new StackGresCluster();
          cluster.setMetadata(new ObjectMetaBuilder()
              .withName("test")
              .build());
          return cluster;
        }).get())
        .withRestoreContext(Optional.empty())
        .build();
  }

  private StackGresClusterContext createStackGresClusterContextWithRestore() {
    return StackGresClusterContext.builder()
        .withCluster(Unchecked.supplier(() -> {
          StackGresCluster cluster = new StackGresCluster();
          cluster.setMetadata(new ObjectMetaBuilder()
              .withName("test")
              .build());
          return cluster;
        }).get())
        .withRestoreContext(Optional.of(StackGresRestoreContext.builder()
            .build()))
        .build();
  }

}
