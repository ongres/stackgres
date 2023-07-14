/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HugePagesMounts implements VolumeMountsProvider<DistributedLogsContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(DistributedLogsContainerContext context) {
    if (Optional.of(context.getDistributedLogsContext().getSource().getSpec())
        .map(StackGresDistributedLogsSpec::getNonProductionOptions)
        .map(StackGresDistributedLogsNonProduction::getDisablePatroniResourceRequirements)
        .orElse(false)) {
      return List.of();
    }

    return Stream.concat(
        Optional.of(context.getDistributedLogsContext().getProfile().getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .map(quantity -> new VolumeMountBuilder()
                .withName(StackGresVolume.HUGEPAGES_2M.getName())
                .withMountPath(ClusterStatefulSetPath.HUGEPAGES_2M_PATH.path())
                .build())
            .stream(),
        Optional.of(context.getDistributedLogsContext().getProfile().getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages1Gi)
            .map(quantity -> new VolumeMountBuilder()
                .withName(StackGresVolume.HUGEPAGES_1G.getName())
                .withMountPath(ClusterStatefulSetPath.HUGEPAGES_1G_PATH.path())
                .build())
            .stream())
        .toList();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(DistributedLogsContainerContext context) {
    return List.of(
        ClusterStatefulSetPath.HUGEPAGES_2M_PATH.envVar(),
        ClusterStatefulSetPath.HUGEPAGES_1G_PATH.envVar()
    );
  }
}
