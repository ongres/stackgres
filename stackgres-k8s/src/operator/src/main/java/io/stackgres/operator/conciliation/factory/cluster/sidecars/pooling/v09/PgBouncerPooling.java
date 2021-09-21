/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.v09;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_LOCAL_OVERRIDE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_SOCKET;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerPgbouncerIni;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.AbstractPgPooling;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;

@Sidecar("connection-pooling")
@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
@RunningContainer(order = 5)
public class PgBouncerPooling extends AbstractPgPooling {

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;
  private final VolumeMountsProvider<ContainerContext> postgresSocket;

  @Inject
  protected PgBouncerPooling(LabelFactoryForCluster<StackGresCluster> labelFactory,
      @ProviderName(CONTAINER_LOCAL_OVERRIDE)
        VolumeMountsProvider<ContainerContext> containerUserOverrideMounts,
      @ProviderName(POSTGRES_SOCKET)
        VolumeMountsProvider<ContainerContext> postgresSocket) {
    super(labelFactory);
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresSocket = postgresSocket;
  }

  @Override
  protected String getImageName() {
    return "docker.io/ongres/pgbouncer:v1.13.0-build-6.0";
  }

  @Override
  protected Map<String, String> getDefaultParameters()  {
    return ImmutableMap.<String, String>builder()
        .put("listen_port", Integer.toString(EnvoyUtil.PG_POOL_PORT))
        .put("unix_socket_dir", ClusterStatefulSetPath.PG_RUN_PATH.path())
        .build();
  }

  @Override
  protected String getConfigFile(Optional<StackGresPoolingConfig> poolingConfig) {
    var newParams = poolingConfig
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerIni)
        .map(StackGresPoolingConfigPgBouncerPgbouncerIni::getParameters)
        .orElseGet(HashMap::new);

    // Blocklist removal
    PgBouncerBlocklist.getBlocklistParameters().forEach(newParams::remove);

    Map<String, String> parameters = poolingConfig
        .map(StackGresPoolingConfig::getStatus)
        .map(StackGresPoolingConfigStatus::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncerStatus::getDefaultParameters)
        .map(HashMap::new)
        .orElseGet(() -> new HashMap<>(PgBouncerDefaultValues.getDefaultValues()));

    parameters.putAll(defaultParameters);
    parameters.putAll(newParams);

    String pgBouncerConfig = parameters.entrySet().stream()
        .map(entry -> entry.getKey() + " = " + entry.getValue())
        .collect(Collectors.joining("\n"));

    return "[databases]\n"
        + " * = port = " + EnvoyUtil.PG_PORT + "\n"
        + "\n"
        + "[pgbouncer]\n"
        + pgBouncerConfig
        + "\n";
  }

  @Override
  protected List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context) {
    return ImmutableList.<VolumeMount>builder()
        .addAll(postgresSocket.getVolumeMounts(context))
        .add(new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.PGBOUNCER.getVolumeName())
            .withMountPath("/etc/pgbouncer")
            .withReadOnly(true)
            .build())
        .addAll(
            containerUserOverrideMounts.getVolumeMounts(context))
        .build();
  }
}
