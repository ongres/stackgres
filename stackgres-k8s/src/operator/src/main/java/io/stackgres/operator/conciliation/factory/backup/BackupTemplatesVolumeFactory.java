/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class BackupTemplatesVolumeFactory implements VolumeFactory<StackGresBackupContext> {

  public static String name(ClusterContext context) {
    final String clusterName = context.getCluster().getMetadata().getName();
    return StackGresVolume.SCRIPT_TEMPLATES.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresBackupContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .build());
  }

  private Volume buildVolume(StackGresBackupContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .withDefaultMode(0444)
            .build())
        .build();
  }

}
