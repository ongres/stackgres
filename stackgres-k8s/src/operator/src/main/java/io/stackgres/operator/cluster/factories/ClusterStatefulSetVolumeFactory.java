/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factories;

import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.KeyToPathBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigSource;
import io.stackgres.operator.patroni.PatroniRestoreSource;
import io.stackgres.operatorframework.factories.VolumesFactory;

@ApplicationScoped
public class ClusterStatefulSetVolumeFactory implements VolumesFactory<StackGresClusterContext> {

  private PatroniRestoreSource restoreSource;

  @Inject
  public ClusterStatefulSetVolumeFactory(PatroniRestoreSource restoreSource) {
    this.restoreSource = restoreSource;
  }

  @Override
  public ImmutableList<Volume> getVolumes(StackGresClusterContext config) {

    final String name = config.getCluster().getMetadata().getName();

    ImmutableList.Builder<Volume> volumeListBuilder = ImmutableList.<Volume>builder().add(
        new VolumeBuilder()
            .withName(ClusterStatefulSet.SOCKET_VOLUME_NAME)
            .withNewEmptyDir()
            .withMedium("Memory")
            .endEmptyDir()
            .build(),
        new VolumeBuilder()
            .withName(ClusterStatefulSet.WAL_G_WRAPPER_VOLUME_NAME)
            .withNewEmptyDir()
            .withMedium("Memory")
            .endEmptyDir()
            .build(),
        new VolumeBuilder()
            .withName(ClusterStatefulSet.WAL_G_RESTORE_WRAPPER_VOLUME_NAME)
            .withNewEmptyDir()
            .withMedium("Memory")
            .endEmptyDir()
            .build()
    );

    config.getBackupConfig().ifPresent(backupConfig -> {

      Optional.ofNullable(backupConfig.getSpec().getStorage().getVolume())
          .ifPresent(volumeStorage -> volumeListBuilder.add(new VolumeBuilder()
              .withName(name + ClusterStatefulSet.BACKUP_SUFFIX)
              .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSource(
                  name + ClusterStatefulSet.BACKUP_SUFFIX, false))
              .build()));

      Optional.ofNullable(backupConfig.getSpec().getStorage().getGcs())
          .ifPresent(gcsStorage -> volumeListBuilder.add(new VolumeBuilder()
              .withName(ClusterStatefulSet.GCS_CREDENTIALS_VOLUME_NAME)
              .withSecret(new SecretVolumeSourceBuilder()
                  .withSecretName(gcsStorage.getCredentials()
                      .getServiceAccountJsonKey().getName())
                  .withItems(new KeyToPathBuilder()
                      .withKey(gcsStorage.getCredentials()
                          .getServiceAccountJsonKey().getKey())
                      .withPath(ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME)
                      .build())
                  .build())
              .build()));
    });

    config.getRestoreConfig().ifPresent(restoreConfig -> {
      volumeListBuilder.add(
          new VolumeBuilder()
              .withName(ClusterStatefulSet.RESTORE_ENTRYPOINT_VOLUME)
              .withNewEmptyDir()
              .withMedium("Memory")
              .endEmptyDir()
              .build());

      StackgresRestoreConfigSource source = restoreSource.getStorageConfig(restoreConfig);

      Optional.ofNullable(source.getStorage().getGcs())
          .ifPresent(gcsStorage -> volumeListBuilder.add(new VolumeBuilder()
              .withName(ClusterStatefulSet.GCS_RESTORE_CREDENTIALS_VOLUME_NAME)
              .withSecret(new SecretVolumeSourceBuilder()
                  .withSecretName(gcsStorage.getCredentials()
                      .getServiceAccountJsonKey().getName())
                  .withItems(new KeyToPathBuilder()
                      .withKey(gcsStorage.getCredentials()
                          .getServiceAccountJsonKey().getKey())
                      .withPath(ClusterStatefulSet.GCS_RESTORE_CREDENTIALS_FILE_NAME)
                      .build())
                  .build())
              .build()));

    });

    return volumeListBuilder.build();
  }
}
