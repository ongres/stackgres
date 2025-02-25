/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HugePagesMounts implements VolumeMountsProvider<ClusterContainerContext> {

  private final DefaultProfileFactory defaultProfileFactory;

  public HugePagesMounts(DefaultProfileFactory defaultProfileFactory) {
    this.defaultProfileFactory = defaultProfileFactory;
  }

  @Override
  public List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    if (context.getClusterContext().calculateDisablePatroniResourceRequirements()) {
      return List.of();
    }

    var profile = context.getClusterContext().getProfile()
        .orElseGet(() -> defaultProfileFactory.buildResource(context.getClusterContext().getSource()));
    return Stream.concat(
        Optional.of(profile.getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .map(quantity -> new VolumeMountBuilder()
                .withName(StackGresVolume.HUGEPAGES_2M.getName())
                .withMountPath(ClusterPath.HUGEPAGES_2M_PATH.path())
                .build())
            .stream(),
        Optional.of(profile.getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages1Gi)
            .map(quantity -> new VolumeMountBuilder()
                .withName(StackGresVolume.HUGEPAGES_1G.getName())
                .withMountPath(ClusterPath.HUGEPAGES_1G_PATH.path())
                .build())
            .stream())
        .toList();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    return List.of(
        ClusterPath.HUGEPAGES_2M_PATH.envVar(),
        ClusterPath.HUGEPAGES_1G_PATH.envVar()
    );
  }
}
