/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.fluentd.v09;

import static io.stackgres.common.FluentdUtil.PATRONI_LOG_TYPE;
import static io.stackgres.common.FluentdUtil.POSTGRES_LOG_TYPE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_LOCAL_OVERRIDE;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.POSTGRES_SOCKET;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
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
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.distributedlogs.PatroniTableFields;
import io.stackgres.common.distributedlogs.PostgresTableFields;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ClusterRunningContainer;
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
import io.stackgres.operator.conciliation.factory.distributedlogs.v09.StatefulSetDynamicVolumes;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
@RunningContainer(ClusterRunningContainer.FLUENTD_V09)
public class Fluentd implements ContainerFactory<DistributedLogsContainerContext>,
    ResourceGenerator<StackGresDistributedLogsContext>,
    VolumeFactory<StackGresDistributedLogsContext> {

  static final String PATRONI_TABLE_FIELDS = Stream.of(PatroniTableFields.values())
      .map(PatroniTableFields::getFieldName)
      .collect(Collectors.joining(","));
  static final String POSTGRES_TABLE_FIELDS = Stream.of(PostgresTableFields.values())
      .map(PostgresTableFields::getFieldName)
      .collect(Collectors.joining(","));
  private static final String IMAGE_NAME = "docker.io/ongres/fluentd:v1.9.3-build-6.0";
  private VolumeMountsProvider<ContainerContext> containerLocalOverrideMounts;
  private VolumeMountsProvider<ContainerContext> postgresSocket;
  private LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  // list of log_patroni table fields
  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.FLUENTD)
        .withImage(IMAGE_NAME)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-exc")
        .withArgs(""
            + "echo 'Wait for postgres to be up, running and initialized!'\n"
            + "until curl -s localhost:8008/read-only --fail > /dev/null; do sleep 1; done\n"
            + "CONFIG_PATH=/etc/fluentd\n"
            + "update_config() {\n"
            + "  rm -Rf /tmp/last_config\n"
            + "  cp -Lr \"$CONFIG_PATH\" /tmp/last_config\n"
            + "}\n"
            + "\n"
            + "has_config_changed() {\n"
            + "  for file in $(ls -1 \"$CONFIG_PATH\")\n"
            + "  do\n"
            + "    [ \"$(cat \"$CONFIG_PATH/$file\" | md5sum)\" \\\n"
            + "      != \"$(cat \"/tmp/last_config/$file\" | md5sum)\" ] \\\n"
            + "      && return || true\n"
            + "  done\n"
            + "  return 1\n"
            + "}\n"
            + "\n"
            + "run_fluentd() {\n"
            + "  set -x\n"
            + "  for database in $(cat \"$CONFIG_PATH/databases\")\n"
            + "  do\n"
            + "    echo \"Create database ${database} if not exists\"\n"
            + "    cat << EOF | ruby -e '\n"
            + "require \"pg\"\n"
            + "conn = PG.connect(host: \"" + ClusterStatefulSetPath.PG_RUN_PATH.path() + "\""
            + ", port: " + EnvoyUtil.PG_PORT + ", user: \"postgres\", dbname: \"postgres\")\n"
            + "conn.exec(STDIN.read)'\n"
            + "CREATE EXTENSION IF NOT EXISTS dblink;\n"
            + "DO\n"
            + "\\$do\\$\n"
            + "BEGIN\n"
            + "   IF EXISTS (SELECT FROM pg_database WHERE datname = '${database}') THEN\n"
            + "      RAISE NOTICE 'Database ${database} already exists';\n"
            + "   ELSE\n"
            + "     PERFORM dblink_exec('host=" + ClusterStatefulSetPath.PG_RUN_PATH.path()
            + " port=" + EnvoyUtil.PG_PORT + " user=postgres dbname=' || current_database()"
            + ", 'CREATE DATABASE \"${database}\"');\n"
            + "   END IF;\n"
            + "END\n"
            + "\\$do\\$;\n"
            + "EOF\n"
            + "  done\n"
            + "  exec /usr/local/bin/fluentd \\\n"
            + "    -c \"$CONFIG_PATH/fluentd.conf\"\n"
            + "}\n"
            + "\n"
            + "set +x\n"
            + "while true\n"
            + "do\n"
            + "  if has_config_changed || [ ! -d \"/proc/$PID\" ]\n"
            + "  then\n"
            + "    echo 'Configuration has changed, restarting fluentd'\n"
            + "    update_config\n"
            + "    if [ -n \"$PID\" ]\n"
            + "    then\n"
            + "      kill \"$PID\" || true\n"
            + "      wait \"$PID\" || true\n"
            + "    fi\n"
            + "    run_fluentd &\n"
            + "    PID=\"$!\"\n"
            + "  fi\n"
            + "  sleep 5\n"
            + "done\n")
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
                .withReadOnly(Boolean.TRUE)
                .build())
        .addAllToVolumeMounts(containerLocalOverrideMounts.getVolumeMounts(context))
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

    final String databaseList = context.getConnectedClusters()
        .stream()
        .map(FluentdUtil::databaseName)
        .collect(Collectors.joining("\n"));

    final Map<String, String> data = ImmutableMap.of(
        "fluentd.conf", getFluentdConfig(context),
        "databases", databaseList);

    final Map<String, String> labels = labelFactory.clusterLabels(cluster);
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(FluentdUtil.configName(cluster))
        .withLabels(labels)
        .endMetadata()
        .withData(data)
        .build();
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();
    final String namespace = cluster.getMetadata().getNamespace();

    final Map<String, String> labels = labelFactory.clusterLabels(cluster);
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
        + "  workers " + (1 + distributedLogsContext.getConnectedClusters().size()) + "\n"
        + "</system>\n"
        + "\n"
        + "<worker 0>\n"
        + "  <source>\n"
        + "    @type forward\n"
        + "    bind 0.0.0.0\n"
        + "    port " + FluentdUtil.FORWARD_PORT + "\n"
        + "  </source>\n"
        + "  \n"
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
        + "  \n"
        + Seq.seq(distributedLogsContext.getConnectedClusters())
        .zipWithIndex()
        .map(t -> t.map2(index -> index + 1))
        .map(t -> ""
            + "  <match " + FluentBit.tagName(t.v1, "*") + ".*.*>\n"
            + "    @type forward\n"
            + "    <server>\n"
            + "      name localhost\n"
            + "      host 127.0.0.1\n"
            + "      port " + (FluentdUtil.FORWARD_PORT + t.v2) + "\n"
            + "    </server>\n"
            + "  </match>\n"
            + "  \n")
        .collect(Collectors.joining("\n"))
        + "  <match *.*.*.*.*>\n"
        + "    @type forward\n"
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
        .map(t -> t.map2(index -> index + 1))
        .map(t -> ""
            + "<worker " + t.v2 + ">\n"
            + "  <source>\n"
            + "    @type forward\n"
            + "    bind 127.0.0.1\n"
            + "    port " + (FluentdUtil.FORWARD_PORT + t.v2) + "\n"
            + "  </source>\n"
            + "  \n"
            + "  <match " + FluentBit.tagName(t.v1, POSTGRES_LOG_TYPE) + ".*.*>\n"
            + "    @type sql\n"
            + "    host /var/run/postgresql\n"
            + "    port " + EnvoyUtil.PG_PORT + "\n"
            + "    database " + FluentdUtil.databaseName(t.v1) + "\n"
            + "    adapter postgresql\n"
            + "    username postgres\n"
            + "    <table>\n"
            + "      table log_postgres\n"
            + "      column_mapping "
            + "'" + POSTGRES_TABLE_FIELDS + "'\n"
            + "    </table>\n"
            + "  </match>\n"
            + "  \n"
            + "  <match " + FluentBit.tagName(t.v1, PATRONI_LOG_TYPE) + ".*.*>\n"
            + "    @type sql\n"
            + "    host /var/run/postgresql\n"
            + "    port " + EnvoyUtil.PG_PORT + "\n"
            + "    database " + FluentdUtil.databaseName(t.v1) + "\n"
            + "    adapter postgresql\n"
            + "    username postgres\n"
            + "    <table>\n"
            + "      table log_patroni\n"
            + "      column_mapping "
            + "'" + PATRONI_TABLE_FIELDS + "'\n"
            + "    </table>\n"
            + "  </match>\n"
            + "</worker>\n"
            + "\n")
        .collect(Collectors.joining("\n"));

  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Inject
  public void setContainerLocalOverrideMounts(
      @ProviderName(CONTAINER_LOCAL_OVERRIDE)
          VolumeMountsProvider<ContainerContext> containerLocalOverrideMounts) {
    this.containerLocalOverrideMounts = containerLocalOverrideMounts;
  }

  @Inject
  public void setPostgresSocket(
      @ProviderName(POSTGRES_SOCKET)
          VolumeMountsProvider<ContainerContext> postgresSocket) {
    this.postgresSocket = postgresSocket;
  }
}
