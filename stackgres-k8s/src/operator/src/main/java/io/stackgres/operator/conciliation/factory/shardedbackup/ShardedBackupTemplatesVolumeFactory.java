/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesVolumeFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ShardedBackupTemplatesVolumeFactory
    extends AbstractTemplatesVolumeFactory
    implements VolumeFactory<StackGresShardedBackupContext> {

  public static String name(StackGresShardedBackupContext context) {
    final String clusterName = context.getShardedCluster().getMetadata().getName();
    return StackGresVolume.SCRIPT_TEMPLATES.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresShardedBackupContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .build());
  }

  private Volume buildVolume(StackGresShardedBackupContext context) {
    return buildVolumeForShardedCluster(context);
  }

}
