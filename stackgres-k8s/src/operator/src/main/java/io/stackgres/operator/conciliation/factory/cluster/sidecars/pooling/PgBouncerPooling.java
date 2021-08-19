/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_SOCKET;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
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
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerBlocklist;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;

@Sidecar("connection-pooling")
@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@RunningContainer(order = 4)
public class PgBouncerPooling extends AbstractPgPooling {

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;
  private final VolumeMountsProvider<ContainerContext> postgresSocket;

  @Inject
  protected PgBouncerPooling(LabelFactoryForCluster<StackGresCluster> labelFactory,
      @ProviderName(CONTAINER_USER_OVERRIDE)
        VolumeMountsProvider<ContainerContext> containerUserOverrideMounts,
      @ProviderName(POSTGRES_SOCKET)
        VolumeMountsProvider<ContainerContext> postgresSocket) {
    super(labelFactory);
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresSocket = postgresSocket;
  }

  @Override
  protected String getConfigFile(Optional<StackGresPoolingConfig> poolingConfig) {
    return ""
        + getDatabaseSection(poolingConfig)
        + getUserSection(poolingConfig)
        + getPgBouncerSection(poolingConfig);
  }

  @Override
  protected List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context) {
    return ImmutableList.<VolumeMount>builder()
        .addAll(postgresSocket.getVolumeMounts(context))
        .add(new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.PG_BOUNCER.getVolumeName())
            .withMountPath("/etc/pgbouncer")
            .withReadOnly(true)
            .build())
        .addAll(
            containerUserOverrideMounts.getVolumeMounts(context))
        .build();
  }

  private String getPgBouncerSection(Optional<StackGresPoolingConfig> poolingConfig) {
    var newParams = poolingConfig
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getParameters)
        .orElseGet(HashMap::new);

    // Blocklist removal
    PgBouncerBlocklist.getBlocklistParameters().forEach(bl -> newParams.remove(bl));

    Map<String, String> parameters = poolingConfig
        .map(StackGresPoolingConfig::getStatus)
        .map(StackGresPoolingConfigStatus::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncerStatus::getDefaultParameters)
        .map(HashMap::new)
        .orElseGet(() -> new HashMap<>(PgBouncerDefaultValues.getDefaultValues()));

    parameters.putAll(DEFAULT_PARAMETERS);
    parameters.putAll(newParams);

    String pgBouncerConfig = parameters.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + " = " + entry.getValue())
        .collect(Collectors.joining("\n"));

    return "[pgbouncer]\n" + pgBouncerConfig + "\n";
  }

  private String getUserSection(Optional<StackGresPoolingConfig> poolingConfig) {
    var users = poolingConfig
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getUsers)
        .orElseGet(HashMap::new);

    return !users.isEmpty()
        ? "[users]\n" + getSections(users) + "\n\n"
        : "";
  }

  private String getDatabaseSection(Optional<StackGresPoolingConfig> poolingConfig) {
    var databases = poolingConfig
        .map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getDatabases)
        .orElseGet(HashMap::new);

    return !databases.isEmpty()
        ? "[databases]\n" + getSections(databases) + "\n\n"
        : "[databases]\n" + "* = port=" + EnvoyUtil.PG_PORT + "\n\n";
  }

  private String getSections(Map<String, Map<String, String>> sections) {
    return sections.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + " = " + entry.getValue().entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(" ")))
        .collect(Collectors.joining("\n"));
  }
}
