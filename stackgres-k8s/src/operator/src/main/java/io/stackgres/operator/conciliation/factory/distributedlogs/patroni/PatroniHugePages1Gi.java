/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class PatroniHugePages1Gi implements VolumeFactory<StackGresDistributedLogsContext> {

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresDistributedLogsContext context) {
    final var profile = context.getProfile();

    return Stream.<VolumePair>of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .build())
        .filter(volumePair -> Optional.of(profile.getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .isPresent());
  }

  public @NotNull Volume buildVolume(StackGresDistributedLogsContext context) {
    return new VolumeBuilder()
        .withName(PatroniStaticVolume.HUGEPAGES_1G.getVolumeName())
        .withEmptyDir(new EmptyDirVolumeSourceBuilder()
            .withMedium("HugePages-1Gi")
            .build())
        .build();
  }

}
