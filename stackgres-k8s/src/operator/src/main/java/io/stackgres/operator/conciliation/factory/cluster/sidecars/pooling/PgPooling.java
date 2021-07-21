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

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncerStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.Blocklist;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.PgBouncerDefaultValues;

@Sidecar("connection-pooling")
@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@RunningContainer(order = 4)
public class PgPooling extends AbstractPgPooling {

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;
  private final VolumeMountsProvider<ContainerContext> postgresSocket;

  @Inject
  protected PgPooling(LabelFactory<StackGresCluster> labelFactory,
                      @ProviderName(CONTAINER_USER_OVERRIDE)
                          VolumeMountsProvider<ContainerContext> containerUserOverrideMounts,
                      @ProviderName(POSTGRES_SOCKET)
                          VolumeMountsProvider<ContainerContext> postgresSocket) {
    super(labelFactory);
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresSocket = postgresSocket;
  }

  @Override
  protected Map<String, String> getParameters(Optional<StackGresPoolingConfig> pgbouncerConfig) {
    Map<String, String> newParams = pgbouncerConfig.map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerConf)
        .orElseGet(HashMap::new);
    for (String bl : Blocklist.getBlocklistParameters()) {
      newParams.remove(bl);
    }
    Map<String, String> params = pgbouncerConfig.map(StackGresPoolingConfig::getStatus)
        .map(StackGresPoolingConfigStatus::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncerStatus::getDefaultParameters)
        .map(HashMap::new)
        .orElseGet(() -> new HashMap<>(PgBouncerDefaultValues.getDefaultValues()));
    for (Map.Entry<String, String> entry : newParams.entrySet()) {
      params.put(entry.getKey(), entry.getValue());
    }
    return params;
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
}
