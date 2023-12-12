/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.backup;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ShardedBackupTemplatesVolumeFactory
    implements VolumeFactory<StackGresShardedClusterContext> {

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresShardedClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .build());
  }

  private Volume buildVolume(StackGresShardedClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(ShardedBackupTemplatesConfigMap.name(context))
            .withDefaultMode(0444)
            .build())
        .build();
  }

}
