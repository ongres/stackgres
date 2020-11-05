/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.distributedlogs.fluentd;

import static io.stackgres.common.FluentdUtil.PATRONI_LOG_TYPE;
import static io.stackgres.common.FluentdUtil.POSTGRES_LOG_TYPE;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
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
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.distributedlogs.PatroniTableFields;
import io.stackgres.common.distributedlogs.PostgresTableFields;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operator.sidecars.fluentbit.FluentBit;
import io.stackgres.operatorframework.resource.factory.ContainerResourceFactory;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Fluentd implements ContainerResourceFactory<Void,
    StackGresDistributedLogsContext> {

  private static final Logger FLEUNTD_LOGGER = LoggerFactory.getLogger("io.stackgres.fleuntd");

  static final String PATRONI_TABLE_FIELDS = Stream.of(PatroniTableFields.values())
      .map(PatroniTableFields::getFieldName)
      .collect(Collectors.joining(","));
  static final String POSTGRES_TABLE_FIELDS = Stream.of(PostgresTableFields.values())
      .map(PostgresTableFields::getFieldName)
      .collect(Collectors.joining(","));

  private LabelFactoryDelegator factoryDelegator;

  // list of log_patroni table fields
  @Override
  public Container getContainer(StackGresDistributedLogsContext context) {
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.FLUENTD)
        .withImage(StackGresComponent.FLUENTD.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-exc")
        .withArgs(""
            + "echo 'Wait for postgres to be up, running and initialized!'\n"
            + "until curl -s localhost:8008/read-only --fail > /dev/null; do sleep 1; done\n"
            + "exec /usr/local/bin/fluentd \\\n"
            + "  -c \"/etc/fluentd/fluentd.conf\"\n")
        .withPorts(
            new ContainerPortBuilder()
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
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.SOCKET
            .volumeMount(context),
            new VolumeMountBuilder()
              .withName(StackgresClusterContainers.FLUENTD)
              .withMountPath("/etc/fluentd")
              .withReadOnly(Boolean.TRUE)
              .build(),
            new VolumeMountBuilder()
              .withName(FluentdUtil.BUFFER)
              .withMountPath("/var/log/fluentd")
              .withReadOnly(Boolean.FALSE)
              .build())
        .build();
  }

  @Override
  public Stream<Container> getInitContainers(StackGresDistributedLogsContext context) {
    return Seq.of(Optional.of(createSetupConfigContainer()))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Container createSetupConfigContainer() {
    return new ContainerBuilder()
        .withName("setup-fluentd-config")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ecx", Stream.of(
            "cp /etc/fluentd/initial-fluentd.conf /fluentd/fluentd.conf")
            .collect(Collectors.joining(" && ")))
        .withVolumeMounts(
            new VolumeMountBuilder()
            .withName(FluentdUtil.CONFIG)
            .withMountPath("/etc/fluentd")
            .withReadOnly(Boolean.TRUE)
            .build(),
            new VolumeMountBuilder()
            .withName(StackgresClusterContainers.FLUENTD)
            .withMountPath("/fluentd")
            .withReadOnly(Boolean.FALSE)
            .build())
        .build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(StackGresDistributedLogsContext context) {
    return ImmutableList.of(
        new VolumeBuilder()
        .withName(FluentdUtil.CONFIG)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(FluentdUtil.configName(
                context.getDistributedLogs()))
            .build())
        .build(),
        new VolumeBuilder()
        .withName(StackgresClusterContainers.FLUENTD)
        .withEmptyDir(new EmptyDirVolumeSource())
        .build(),
        new VolumeBuilder()
        .withName(FluentdUtil.BUFFER)
        .withEmptyDir(new EmptyDirVolumeSource())
        .build());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs distributedLogs = context.getDistributedLogs();
    final String namespace = distributedLogs.getMetadata().getNamespace();

    final Map<String, String> data = ImmutableMap.of(
        "fluentd.conf", getFluentdConfig(context, true),
        "initial-fluentd.conf", getFluentdConfig(context, false));

    final StackGresClusterContext clusterContext = context;
    final StackGresCluster cluster = clusterContext.getCluster();
    final LabelFactory<?> labelFactory = factoryDelegator
        .pickFactory(clusterContext);
    final Map<String, String> clusterLabels = labelFactory
        .clusterLabels(cluster);
    final ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(FluentdUtil.configName(context.getDistributedLogs()))
        .withLabels(clusterLabels)
        .withOwnerReferences(clusterContext.getOwnerReferences())
        .endMetadata()
        .withData(data)
        .build();

    final Map<String, String> patroniPrimaryLabels = labelFactory
        .patroniPrimaryLabels(cluster);
    final Service service = new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(FluentdUtil.serviceName(context.getDistributedLogs()))
        .withLabels(patroniPrimaryLabels)
        .withOwnerReferences(clusterContext.getOwnerReferences())
        .endMetadata()
        .withNewSpec()
        .withSelector(patroniPrimaryLabels)
        .withPorts(new ServicePortBuilder()
            .withProtocol("TCP")
            .withName(FluentdUtil.FORWARD_PORT_NAME)
            .withPort(FluentdUtil.FORWARD_PORT)
            .withTargetPort(new IntOrString(FluentdUtil.FORWARD_PORT_NAME))
            .build())
        .withType("ClusterIP")
        .endSpec()
        .build();

    return Seq.of(configMap, service);
  }

  private String getFluentdConfig(final StackGresDistributedLogsContext distributedLogsContext,
      boolean includeClusters) {
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
            .filter(cluster -> includeClusters)
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
        + "      path /var/log/fluentd/loop\n"
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
            .filter(cluster -> includeClusters)
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
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
