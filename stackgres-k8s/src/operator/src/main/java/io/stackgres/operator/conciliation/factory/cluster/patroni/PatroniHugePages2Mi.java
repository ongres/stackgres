/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class PatroniHugePages2Mi implements VolumeFactory<StackGresClusterContext> {

  private final DefaultProfileFactory defaultProfileFactory;

  public PatroniHugePages2Mi(DefaultProfileFactory defaultProfileFactory) {
    this.defaultProfileFactory = defaultProfileFactory;
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    if (context.calculateDisablePatroniResourceRequirements()) {
      return Stream.of();
    }

    final var profile = context.getProfile()
        .orElseGet(() -> defaultProfileFactory.buildResource(context.getSource()));

    return Stream.<VolumePair>of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .build())
        .filter(volumePair -> Optional.of(profile.getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .isPresent());
  }

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.HUGEPAGES_2M.getName())
        .withEmptyDir(new EmptyDirVolumeSourceBuilder()
            .withMedium("HugePages-2Mi")
            .build())
        .build();
  }

}
