/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.fluentbit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractFluentBit implements
    ContainerFactory<StackGresClusterContainerContext>,
    VolumeFactory<StackGresClusterContext> {
  public static final String NAME = StackgresClusterContainers.FLUENT_BIT;

  private static final String CONFIG_SUFFIX = "-fluent-bit";

  protected final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public AbstractFluentBit(
      LabelFactoryForCluster<StackGresCluster> labelFactory) {
    super();
    this.labelFactory = labelFactory;
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + CONFIG_SUFFIX);
  }

  public static String tagName(StackGresCluster cluster, String suffix) {
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    return namespace + "." + name + "." + suffix;
  }

  @Override
  public boolean isActivated(StackGresClusterContainerContext context) {
    return context.getClusterContext().getSource().getSpec().getDistributedLogs() != null;
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContainerContext context) {
    return ImmutableMap.of(
        StackGresContext.FLUENTBIT_VERSION_KEY,
        StackGresComponent.FLUENT_BIT.get(context.getClusterContext().getCluster())
        .findLatestVersion());
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(NAME)
        .withImage(StackGresComponent.FLUENT_BIT.get(context.getClusterContext().getCluster())
            .findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh", "-exc")
        .withArgs("""
            CONFIG_PATH=/etc/fluent-bit
            update_config() {
              rm -Rf "$PG_LOG_PATH/last_config"
              cp -Lr "$CONFIG_PATH" "$PG_LOG_PATH/last_config"
            }

            has_config_changed() {
              for file in $(ls -1 "$CONFIG_PATH")
              do
                [ "$(cat "$CONFIG_PATH/$file" | md5sum)" \\
                  != "$(cat "$PG_LOG_PATH/last_config/$file" | md5sum)" ] \\
                  && return || true
              done
              return 1
            }

            run_fluentbit() {
              set -x
              exec /usr/local/bin/fluent-bit \\
                -c /etc/fluent-bit/fluentbit.conf
            }

            set +x
            while true
            do
              if has_config_changed || [ ! -d "/proc/$PID" ]
              then
                update_config
                if [ -n "$PID" ]
                then
                  kill "$PID"
                  wait "$PID" || true
                fi
                run_fluentbit &
                PID="$!"
              fi
              sleep 5
            done
            """)
        .withEnv(getContainerEnvironmentVariables(context))
        .withVolumeMounts(getVolumeMounts(context))
        .build();
  }

  protected abstract List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context);

  protected abstract List<EnvVar> getContainerEnvironmentVariables(
      StackGresClusterContainerContext context);

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(ImmutableVolumePair.builder()
        .volume(buildVolume(context))
        .source(buildSource(context))
        .build());
  }

  protected Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StatefulSetDynamicVolumes.FLUENT_BIT.getVolumeName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build();
  }

  protected abstract Optional<HasMetadata> buildSource(StackGresClusterContext context);

}
