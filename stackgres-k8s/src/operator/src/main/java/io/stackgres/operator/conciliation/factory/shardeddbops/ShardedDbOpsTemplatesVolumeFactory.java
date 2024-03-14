/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.ShardedClusterContext;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesVolumeFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ShardedDbOpsTemplatesVolumeFactory
    extends AbstractTemplatesVolumeFactory
    implements VolumeFactory<StackGresShardedDbOpsContext> {

  public static String name(ShardedClusterContext context) {
    final String clusterName = context.getShardedCluster().getMetadata().getName();
    return StackGresVolume.SCRIPT_TEMPLATES.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresShardedDbOpsContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .build());
  }

  private Volume buildVolume(StackGresShardedDbOpsContext context) {
    return buildVolumeForShardedCluster(context);
  }

}
