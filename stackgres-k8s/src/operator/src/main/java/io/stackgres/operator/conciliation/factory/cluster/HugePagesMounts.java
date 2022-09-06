/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;

@ApplicationScoped
public class HugePagesMounts implements VolumeMountsProvider<ClusterContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    if (Optional.of(context.getClusterContext().getSource().getSpec())
        .map(StackGresClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getDisablePatroniResourceRequirements)
        .orElse(false)) {
      return List.of();
    }

    return Stream.concat(
        Optional.of(context.getClusterContext().getProfile().getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages2Mi)
            .map(quantity -> new VolumeMountBuilder()
                .withName(PatroniStaticVolume.HUGEPAGES_2M.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.HUGEPAGES_2M_PATH.path())
                .build())
            .stream(),
        Optional.of(context.getClusterContext().getProfile().getSpec())
            .map(StackGresProfileSpec::getHugePages)
            .map(StackGresProfileHugePages::getHugepages1Gi)
            .map(quantity -> new VolumeMountBuilder()
                .withName(PatroniStaticVolume.HUGEPAGES_1G.getVolumeName())
                .withMountPath(ClusterStatefulSetPath.HUGEPAGES_1G_PATH.path())
                .build())
            .stream())
        .toList();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    return List.of(
        ClusterStatefulSetPath.HUGEPAGES_2M_PATH.envVar(),
        ClusterStatefulSetPath.HUGEPAGES_1G_PATH.envVar()
    );
  }
}
