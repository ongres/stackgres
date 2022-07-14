/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.fluentd;

import static io.stackgres.common.FluentdUtil.PATRONI_LOG_TYPE;
import static io.stackgres.common.FluentdUtil.POSTGRES_LOG_TYPE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_SOCKET;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.TCPSocketActionBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.distributedlogs.PatroniTableFields;
import io.stackgres.common.distributedlogs.PostgresTableFields;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.fluentbit.FluentBit;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.FluentdStaticVolume;
import io.stackgres.operator.conciliation.factory.distributedlogs.StatefulSetDynamicVolumes;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainer.FLUENTD)
public class Fluentd implements ContainerFactory<DistributedLogsContainerContext>,
    ResourceGenerator<StackGresDistributedLogsContext>,
    VolumeFactory<StackGresDistributedLogsContext> {

  static final String PATRONI_TABLE_FIELDS = Stream.of(PatroniTableFields.values())
      .map(PatroniTableFields::getFieldName)
      .collect(Collectors.joining(","));
  static final String POSTGRES_TABLE_FIELDS = Stream.of(PostgresTableFields.values())
      .map(PostgresTableFields::getFieldName)
      .collect(Collectors.joining(","));
  private static final Logger FLEUNTD_LOGGER = LoggerFactory.getLogger("io.stackgres.fleuntd");
  private VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;
  private VolumeMountsProvider<ContainerContext> postgresSocket;
  private LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  @Override
  public Map<String, String> getComponentVersions(DistributedLogsContainerContext context) {
    return ImmutableMap.of(
        StackGresContext.FLUENTD_VERSION_KEY,
        StackGresComponent.FLUENTD.get(context.getDistributedLogsContext().getSource())
        .getLatestVersion());
  }

  // list of log_patroni table fields
  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresContainer.FLUENTD.getName())
        .withImage(StackGresComponent.FLUENTD.get(context.getDistributedLogsContext().getSource())
            .getLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-exc")
        .withArgs(""
            + "echo 'Wait for postgres to be up, running and initialized'\n"
            + "until curl -s localhost:8008/read-only --fail > /dev/null; do sleep 1; done\n"
            + "exec /usr/local/bin/fluentd -c /etc/fluentd/fluentd.conf\n")
        .withPorts(
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withName(FluentdUtil.FORWARD_PORT_NAME)
                .withContainerPort(FluentdUtil.FORWARD_PORT).build())
        .withLivenessProbe(new ProbeBuilder()
            .withTcpSocket(new TCPSocketActionBuilder()
                .withPort(new IntOrString(FluentdUtil.FORWARD_PORT))
                .build())
            .withInitialDelaySeconds(15)
            .withPeriodSeconds(20)
            .withFailureThreshold(6)
            .build())
        .withReadinessProbe(new ProbeBuilder()
            .withTcpSocket(new TCPSocketActionBuilder()
                .withPort(new IntOrString(FluentdUtil.FORWARD_PORT))
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(10)
            .build())
        .addAllToVolumeMounts(postgresSocket.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
                .withName(StatefulSetDynamicVolumes.FLUENTD_CONFIG.getVolumeName())
                .withMountPath("/etc/fluentd")
                .withReadOnly(Boolean.FALSE)
                .build(),
            new VolumeMountBuilder()
                .withName(FluentdStaticVolume.FLUENTD_BUFFER.getVolumeName())
                .withMountPath("/var/log/fluentd")
                .build())
        .addAllToVolumeMounts(containerUserOverrideMounts.getVolumeMounts(context))
        .build();

  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresDistributedLogsContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build()
    );
  }

  public @NotNull Volume buildVolume(StackGresDistributedLogsContext context) {
    return new VolumeBuilder()
        .withName(StatefulSetDynamicVolumes.FLUENTD_CONFIG.getVolumeName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(FluentdUtil.configName(
                context.getSource()))
            .withDefaultMode(420)
            .build())
        .build();
  }

  public @NotNull HasMetadata buildSource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();
    final String namespace = cluster.getMetadata().getNamespace();

    final Map<String, String> data = ImmutableMap.of(
        "fluentd.conf", getFluentdConfig(context));

    final Map<String, String> labels = labelFactory.genericLabels(cluster);
    final ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(FluentdUtil.configName(cluster))
        .withLabels(labels)
        .endMetadata()
        .withData(data)
        .build();
    return configMap;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();
    final String namespace = cluster.getMetadata().getNamespace();

    final Map<String, String> labels = labelFactory.genericLabels(cluster);
    final Service service = new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(FluentdUtil.serviceName(cluster))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withSelector(labels)
        .withPorts(new ServicePortBuilder()
            .withProtocol("TCP")
            .withName(FluentdUtil.FORWARD_PORT_NAME)
            .withPort(FluentdUtil.FORWARD_PORT)
            .withTargetPort(new IntOrString(FluentdUtil.FORWARD_PORT_NAME))
            .build())
        .withType("ClusterIP")
        .endSpec()
        .build();

    return Seq.of(service);
  }

  private String getFluentdConfig(final StackGresDistributedLogsContext distributedLogsContext) {
    return ""
        + "<system>\n"
        + "  workers " + getMaxWorkersBeforeRestart(distributedLogsContext) + "\n"
        + "</system>\n"
        + "\n"
        + "<worker 0>\n"
        + "  <source>\n"
        + "    @type forward\n"
        + "    bind 0.0.0.0\n"
        + "    port " + FluentdUtil.FORWARD_PORT + "\n"
        + "  </source>\n"
        + "\n"
        + "  <filter *.*.*.*.*>\n"
        + "    @type record_transformer\n"
        + "    enable_ruby\n"
        + "    <record>\n"
        + "      pod_name ${record[\"kubernetes\"][\"pod_name\"]}\n"
        + "    </record>\n"
        + "    <record>\n"
        + "      role ${record[\"kubernetes\"][\"labels\"][\"role\"]}\n"
        + "    </record>\n"
        + "  </filter>"
        + "\n"
        + Seq.seq(distributedLogsContext.getConnectedClusters())
        .zipWithIndex()
        .map(t -> t.map2(index -> index + getCoreWorkers()))
        .map(t -> ""
            + "  <match " + FluentBit.tagName(t.v1, "*") + ".*.*>\n"
            + "    @type forward\n"
            + "    <buffer>\n"
            + "      @type file\n"
            + "      path /var/log/fluentd/" + FluentBit.tagName(t.v1, "buffer") + "\n"
            + "    </buffer>\n"
            + "    <server>\n"
            + "      name localhost\n"
            + "      host 127.0.0.1\n"
            + "      port " + (FluentdUtil.FORWARD_PORT + t.v2) + "\n"
            + "    </server>\n"
            + "  </match>\n"
            + "\n")
        .collect(Collectors.joining("\n"))
        + "  <match *.*.*.*.*>\n"
        + "    @type forward\n"
        + "    <buffer>\n"
        + "      @type file\n"
        + "      path /var/log/fluentd/loop.buffer\n"
        + "    </buffer>\n"
        + "    <server>\n"
        + "      name localhost\n"
        + "      host 127.0.0.1\n"
        + "      port " + FluentdUtil.FORWARD_PORT + "\n"
        + "    </server>\n"
        + "  </match>\n"
        + "</worker>\n"
        + "\n"
        + Seq.seq(distributedLogsContext.getConnectedClusters())
        .zipWithIndex()
        .map(t -> t.map2(index -> index + getCoreWorkers()))
        .map(t -> ""
            + "<worker " + t.v2 + ">\n"
            + "  <source>\n"
            + "    @type forward\n"
            + "    bind 127.0.0.1\n"
            + "    port " + (FluentdUtil.FORWARD_PORT + t.v2) + "\n"
            + "  </source>\n"
            + "\n"
            + "  <match " + FluentBit.tagName(t.v1, POSTGRES_LOG_TYPE) + ".*.*>\n"
            + "    @type copy\n"
            + "    <store>\n"
            + "      @type sql\n"
            + "      host /var/run/postgresql\n"
            + "      port " + EnvoyUtil.PG_PORT + "\n"
            + "      database " + FluentdUtil.databaseName(t.v1) + "\n"
            + "      adapter postgresql\n"
            + "      username postgres\n"
            + "      <table>\n"
            + "        table log_postgres\n"
            + "        column_mapping '" + POSTGRES_TABLE_FIELDS + "'\n"
            + "      </table>\n"
            + "    </store>\n"
            + "    <store>\n"
            + "      @type stdout\n"
            + "      @log_level " + (FLEUNTD_LOGGER.isTraceEnabled() ? "info" : "debug") + "\n"
            + "    </store>\n"
            + "  </match>\n"
            + "\n"
            + "  <match " + FluentBit.tagName(t.v1, PATRONI_LOG_TYPE) + ".*.*>\n"
            + "    @type copy\n"
            + "    <store>\n"
            + "      @type sql\n"
            + "      host /var/run/postgresql\n"
            + "      port " + EnvoyUtil.PG_PORT + "\n"
            + "      database " + FluentdUtil.databaseName(t.v1) + "\n"
            + "      adapter postgresql\n"
            + "      username postgres\n"
            + "      <table>\n"
            + "        table log_patroni\n"
            + "        column_mapping '" + PATRONI_TABLE_FIELDS + "'\n"
            + "      </table>\n"
            + "    </store>\n"
            + "    <store>\n"
            + "      @type stdout\n"
            + "      @log_level " + (FLEUNTD_LOGGER.isTraceEnabled() ? "info" : "debug") + "\n"
            + "    </store>\n"
            + "  </match>\n"
            + "</worker>\n"
            + "\n")
        .collect(Collectors.joining("\n"));
  }

  /**
   * When needed workers are more than 16 fluentd must be restarted.
   */
  private int getMaxWorkersBeforeRestart(
      final StackGresDistributedLogsContext distributedLogsContext) {
    return getWorkersBatchSize()
        * ((getCoreWorkers() + distributedLogsContext.getConnectedClusters().size()
        + getWorkersBatchSize() - 1) / getWorkersBatchSize());
  }

  private int getCoreWorkers() {
    return 1;
  }

  private int getWorkersBatchSize() {
    return 16;
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Inject
  public void setContainerUserOverrideMounts(
      @ProviderName(CONTAINER_USER_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerUserOverrideMounts) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
  }

  @Inject
  public void setPostgresSocket(
      @ProviderName(POSTGRES_SOCKET)
          VolumeMountsProvider<ContainerContext> postgresSocket) {
    this.postgresSocket = postgresSocket;
  }
}
