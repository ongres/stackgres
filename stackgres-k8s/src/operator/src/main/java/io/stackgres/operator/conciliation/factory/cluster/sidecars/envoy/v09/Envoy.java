/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy.v09;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_LOCAL_OVERRIDE;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy.AbstractEnvoy;

@Singleton
@Sidecar(AbstractEnvoy.NAME)
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
@RunningContainer(order = 2)
public class Envoy extends AbstractEnvoy {

  private final VolumeMountsProvider<ContainerContext> containerLocalOverrideMounts;

  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider,
               LabelFactoryForCluster<StackGresCluster> labelFactory,
               @ProviderName(CONTAINER_LOCAL_OVERRIDE)
                     VolumeMountsProvider<ContainerContext> containerLocalOverrideMounts) {
    super(yamlMapperProvider, labelFactory);
    this.containerLocalOverrideMounts = containerLocalOverrideMounts;
  }

  protected Stream<ImmutableVolumePair> buildExtraVolumes(StackGresClusterContext context) {
    return Stream.of();
  }

  @Override
  protected String getEnvoyConfigPath(final StackGresCluster stackGresCluster,
      boolean disablePgBouncer) {
    final String envoyConfPath;
    if (disablePgBouncer) {
      envoyConfPath = "/envoy/envoy_nopgbouncer.yaml";
    } else {
      envoyConfPath = "/envoy/default_envoy.yaml";
    }
    return envoyConfPath;
  }

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context) {
    return containerLocalOverrideMounts.getVolumeMounts(context);
  }

  @Override
  public String getImageName() {
    return "docker.io/ongres/envoy:v1.15.3-build-6.0";
  }
}
