/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.distributedlogs.fluentd;

import static io.stackgres.common.FluentdUtil.PATRONI_LOG_TYPE;
import static io.stackgres.common.FluentdUtil.POSTGRES_LOG_TYPE;

import java.util.Map;
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
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.distributedlogs.PatroniTableFields;
import io.stackgres.common.distributedlogs.PostgresTableFields;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetPath;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresDistributedLogsContext;
import io.stackgres.operator.common.StackGresDistributedLogsGeneratorContext;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operator.sidecars.fluentbit.FluentBit;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.operatorframework.resource.factory.ContainerResourceFactory;
import org.jooq.lambda.Seq;

@Singleton
public class Fluentd implements ContainerResourceFactory<StackGresDistributedLogs,
    StackGresDistributedLogsGeneratorContext, StackGresDistributedLogs> {
  public static final String IMAGE_NAME = "docker.io/ongres/fluentd:v%s-build-%s";
  static final String DEFAULT_VERSION = StackGresComponents.get("fluentd");
  static final String PATRONI_TABLE_FIELDS = Stream.of(PatroniTableFields.values())
      .map(PatroniTableFields::getFieldName)
      .collect(Collectors.joining(","));
  static final String POSTGRES_TABLE_FIELDS = Stream.of(PostgresTableFields.values())
      .map(PostgresTableFields::getFieldName)
      .collect(Collectors.joining(","));

  private static final String SUFFIX = "-fluentd";

  private LabelFactoryDelegator factoryDelegator;

  public static String configName(StackGresDistributedLogsContext context) {
    return ResourceUtil.resourceName(context.getDistributedLogs().getMetadata().getName() + SUFFIX);
  }

  public static String serviceName(StackGresDistributedLogsContext context) {
    return serviceName(context.getDistributedLogs().getMetadata().getName());
  }

  public static String serviceName(String distributedLogsName) {
    return ResourceUtil.resourceName(distributedLogsName + SUFFIX);
  }

  // list of log_patroni table fields
  @Override
  public Container getContainer(StackGresDistributedLogsGeneratorContext context) {
    return new ContainerBuilder()
      .withName(FluentdUtil.NAME)
      .withImage(String.format(IMAGE_NAME, DEFAULT_VERSION, StackGresContext.CONTAINER_BUILD))
      .withImagePullPolicy("IfNotPresent")
      .withCommand("/bin/sh", "-exc")
      .withArgs(""
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
            + "    cat << EOF | ruby -e '\n"
            + "require \"pg\"\n"
            + "conn = PG.connect(host: \"" + ClusterStatefulSetPath.PG_RUN_PATH.path() + "\""
            + ", port: " + Envoy.PG_PORT + ", user: \"postgres\", dbname: \"postgres\")\n"
            + "conn.exec(STDIN.read)'\n"
            + "CREATE EXTENSION IF NOT EXISTS dblink;\n"
            + "DO\n"
            + "\\$do\\$\n"
            + "BEGIN\n"
            + "   IF EXISTS (SELECT FROM pg_database WHERE datname = '${database}') THEN\n"
            + "      RAISE NOTICE 'Database ${database} already exists';\n"
            + "   ELSE\n"
            + "     PERFORM dblink_exec('host=" + ClusterStatefulSetPath.PG_RUN_PATH.path()
            + " port=" + Envoy.PG_PORT + " user=postgres dbname=' || current_database()"
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
          .volumeMount(context.getClusterContext()),
          new VolumeMountBuilder()
          .withName(FluentdUtil.NAME)
          .withMountPath("/etc/fluentd")
          .withReadOnly(Boolean.TRUE)
          .build())
      .build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(StackGresDistributedLogsGeneratorContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(FluentdUtil.NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context.getDistributedLogsContext()))
            .build())
        .build());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresDistributedLogsGeneratorContext context) {
    final StackGresDistributedLogsContext distributedLogsContext = context
        .getDistributedLogsContext();
    final StackGresDistributedLogs distributedLogs =
        distributedLogsContext.getDistributedLogs();
    final String namespace = distributedLogs.getMetadata().getNamespace();

    final String configFile = ""
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
                + "    port " + Envoy.PG_PORT + "\n"
                + "    database " + FluentdUtil.databaseName(t.v1) + "\n"
                + "    adapter postgresql\n"
                + "    username postgres\n"
                + "    <table>\n"
                + "      table log_postgres\n"
                + "      column_mapping "
                  +   "'" + POSTGRES_TABLE_FIELDS + "'\n"
                + "    </table>\n"
                + "  </match>\n"
                + "  \n"
                + "  <match " + FluentBit.tagName(t.v1, PATRONI_LOG_TYPE) + ".*.*>\n"
                + "    @type sql\n"
                + "    host /var/run/postgresql\n"
                + "    port " + Envoy.PG_PORT + "\n"
                + "    database " + FluentdUtil.databaseName(t.v1) + "\n"
                + "    adapter postgresql\n"
                + "    username postgres\n"
                + "    <table>\n"
                + "      table log_patroni\n"
                + "      column_mapping "
                  +   "'" + PATRONI_TABLE_FIELDS + "'\n"
                + "    </table>\n"
                + "  </match>\n"
                + "</worker>\n"
                + "\n")
            .collect(Collectors.joining("\n"));
    final String databaseList = distributedLogsContext.getConnectedClusters()
        .stream()
        .map(FluentdUtil::databaseName)
        .collect(Collectors.joining("\n"));
    final Map<String, String> data = ImmutableMap.of(
        "fluentd.conf", configFile,
        "databases", databaseList);

    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();
    final LabelFactory<?> labelFactory = factoryDelegator
        .pickFactory(clusterContext);
    final Map<String, String> clusterLabels = labelFactory
        .clusterLabels(cluster);
    final ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configName(distributedLogsContext))
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
        .withName(serviceName(distributedLogsContext))
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

  public StackGresClusterSidecarResourceFactory<Void> toStackGresClusterSidecarResourceFactory() {
    return new FluentdStackGresClusterSidecarResourceFactory();
  }

  private class FluentdStackGresClusterSidecarResourceFactory
      implements StackGresClusterSidecarResourceFactory<Void> {

    @Override
    public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
      return Fluentd.this.streamResources((StackGresDistributedLogsGeneratorContext) context);
    }

    @Override
    public ImmutableList<Volume> getVolumes(StackGresGeneratorContext context) {
      return Fluentd.this.getVolumes((StackGresDistributedLogsGeneratorContext) context);
    }

    @Override
    public Container getContainer(StackGresGeneratorContext context) {
      return Fluentd.this.getContainer((StackGresDistributedLogsGeneratorContext) context);
    }
  }

  @Inject
  public void setFactoryDelegator(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }
}
