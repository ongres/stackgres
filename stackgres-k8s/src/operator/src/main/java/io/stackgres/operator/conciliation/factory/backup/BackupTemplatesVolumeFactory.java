/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesVolumeFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.enterprise.context.ApplicationScoped;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class BackupTemplatesVolumeFactory
    extends AbstractTemplatesVolumeFactory
    implements VolumeFactory<StackGresBackupContext> {

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresBackupContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .build());
  }

  private Volume buildVolume(StackGresBackupContext context) {
    return buildVolumeForCluster(context);
  }

}
