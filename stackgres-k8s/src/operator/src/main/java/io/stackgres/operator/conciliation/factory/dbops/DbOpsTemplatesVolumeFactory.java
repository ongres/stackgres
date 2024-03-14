/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesVolumeFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class DbOpsTemplatesVolumeFactory extends AbstractTemplatesVolumeFactory
    implements VolumeFactory<StackGresDbOpsContext> {

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresDbOpsContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .build());
  }

  private Volume buildVolume(StackGresDbOpsContext context) {
    return buildVolumeForCluster(context);
  }

}
