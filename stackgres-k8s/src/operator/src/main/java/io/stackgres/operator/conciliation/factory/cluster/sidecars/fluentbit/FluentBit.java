/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.fluentbit;

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
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterStatefulSetVolumeConfig;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sidecar(FluentBit.NAME)
@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
@RunningContainer(order = 2)
public class FluentBit implements ContainerFactory<StackGresClusterContext>,
    ResourceGenerator<StackGresClusterContext> {

  public static final String NAME = StackgresClusterContainers.FLUENT_BIT;

  private static final Logger FLUENT_BIT_LOGGER = LoggerFactory.getLogger(
      "io.stackgres.fluent-bit");
  private static final String CONFIG_SUFFIX = "-fluent-bit";

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public FluentBit(ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
                   LabelFactory<StackGresCluster> labelFactory) {
    super();
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
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
  public boolean isActivated(StackGresClusterContext context) {
    return context.getSource().getSpec().getDistributedLogs() != null;
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContext context) {
    return ImmutableMap.of(
        StackGresContext.FLUENTBIT_VERSION_KEY,
        StackGresComponent.FLUENT_BIT.findLatestVersion());
  }

  @Override
  public Container getContainer(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName(NAME)
        .withImage(StackGresComponent.FLUENT_BIT.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh", "-exc")
        .withArgs(""
            + "CONFIG_PATH=/etc/fluent-bit\n"
            + "update_config() {\n"
            + "  rm -Rf \"$PG_LOG_PATH/last_config\"\n"
            + "  cp -Lr \"$CONFIG_PATH\" \"$PG_LOG_PATH/last_config\"\n"
            + "}\n"
            + "\n"
            + "has_config_changed() {\n"
            + "  for file in $(ls -1 \"$CONFIG_PATH\")\n"
            + "  do\n"
            + "    [ \"$(cat \"$CONFIG_PATH/$file\" | md5sum)\" \\\n"
            + "      != \"$(cat \"$PG_LOG_PATH/last_config/$file\" | md5sum)\" ] \\\n"
            + "      && return || true\n"
            + "  done\n"
            + "  return 1\n"
            + "}\n"
            + "\n"
            + "run_fluentbit() {\n"
            + "  set -x\n"
            + "  exec /usr/local/bin/fluent-bit \\\n"
            + "    -c /etc/fluent-bit/fluentbit.conf\n"
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
            + "      kill \"$PID\"\n"
            + "      wait \"$PID\" || true\n"
            + "    fi\n"
            + "    run_fluentbit &\n"
            + "    PID=\"$!\"\n"
            + "  fi\n"
            + "  sleep 5\n"
            + "done\n")
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context.getSource()))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.LOG.volumeMount(context),
            new VolumeMountBuilder()
            .withName(NAME)
            .withMountPath("/etc/fluent-bit")
            .withReadOnly(Boolean.TRUE)
            .build())
        .addAllToVolumeMounts(ClusterStatefulSetVolumeConfig.USER.volumeMounts(context))
        .build();
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    if (cluster.getSpec().getDistributedLogs() == null) {
      return Stream.of();
    }
    final String namespace = cluster.getMetadata().getNamespace();
    final String fluentdRelativeId = cluster.getSpec()
        .getDistributedLogs().getDistributedLogs();
    final String fluentdNamespace =
        StackGresUtil.getNamespaceFromRelativeId(fluentdRelativeId, namespace);
    final String fluentdServiceName = FluentdUtil.serviceName(
        StackGresUtil.getNameFromRelativeId(fluentdRelativeId));

    String parsersConfigFile = ""
        + "[PARSER]\n"
        + "    Name        postgreslog_firstline\n"
        + "    Format      regex\n"
        + "    Regex       "
        + "^(?<log_time>\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}.\\d*\\s\\S{3})"
        + ",(?<message>.*)\n"
        + "\n"
        + "[PARSER]\n"
        + "    Name        postgreslog_1\n"
        + "    Format      regex\n"
        + "    Regex       "
        + "^(?<log_time>\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}.\\d*\\s\\S{3})"
        + ",(?<message>\"([^\"]*(?:\"\"[^\"]*)*)\"|)\n"
        + "\n"
        + "[PARSER]\n"
        + "    Name        patronilog_firstline\n"
        + "    Format      regex\n"
        + "    Regex       "
        + "^(?<log_time>\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2},\\d*{3})"
        + " (?<error_severity>[^:]+): (?<message>.*)\n"
        + "\n"
        + "[PARSER]\n"
        + "    Name        patronilog_1\n"
        + "    Format      regex\n"
        + "    Regex       "
        + "^(?<log_time>\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2},\\d*{3})"
        + " (?<error_severity>[^:]+): (?<message>.*)\n"
        + "\n"
        + "[PARSER]\n"
        + "    Name        kubernetes_tag\n"
        + "    Format      regex\n"
        + "    Regex       ^[^.]+\\.[^.]+\\.[^.]+\\."
        + "(?<namespace_name>[^.]+)\\.(?<pod_name>[^.]+)$\n"
        + "\n";
    final String clusterNamespace = labelFactory.clusterNamespace(cluster);
    String fluentBitConfigFile = ""
        + "[SERVICE]\n"
        + "    Parsers_File      /etc/fluent-bit/parsers.conf\n"
        + "    Log_Level         " + (FLUENT_BIT_LOGGER.isTraceEnabled()
            ? "trace" : FLUENT_BIT_LOGGER.isDebugEnabled()
                ? "debug" : "info") + "\n"
        + "\n"
        + "[INPUT]\n"
        + "    Name              tail\n"
        + "    Path              " + ClusterStatefulSetPath.PG_LOG_PATH.path() + "/postgres*.csv\n"
        + "    Tag               " + FluentdUtil.POSTGRES_LOG_TYPE + "\n"
        + "    DB                " + ClusterStatefulSetPath.PG_LOG_PATH.path() + "/postgreslog.db\n"
        + "    Multiline         On\n"
        + "    Parser_Firstline  postgreslog_firstline\n"
        + "    Parser_1          postgreslog_1\n"
        + "    Buffer_Max_Size   2M\n"
        + "    Skip_Long_Lines   On\n"
        + "\n"
        + "[INPUT]\n"
        + "    Name              tail\n"
        + "    Key               message\n"
        + "    Path              " + ClusterStatefulSetPath.PG_LOG_PATH.path() + "/patroni*.log\n"
        + "    Tag               " + FluentdUtil.PATRONI_LOG_TYPE + "\n"
        + "    DB                " + ClusterStatefulSetPath.PG_LOG_PATH.path() + "/patronilog.db\n"
        + "    Multiline         On\n"
        + "    Parser_Firstline  patronilog_firstline\n"
        + "    Parser_1          patronilog_1\n"
        + "    Buffer_Max_Size   2M\n"
        + "    Skip_Long_Lines   On\n"
        + "\n"
        + "[FILTER]\n"
        + "    Name         rewrite_tag\n"
        + "    Match        " + FluentdUtil.POSTGRES_LOG_TYPE + "\n"
        + "    Rule         $message ^.*$ "
        + tagName(cluster, FluentdUtil.POSTGRES_LOG_TYPE)
        + "." + clusterNamespace + ".${HOSTNAME} false\n"
        + "    Emitter_Name postgres_re_emitted"
        + "\n"
        + "[FILTER]\n"
        + "    Name         rewrite_tag\n"
        + "    Match        " + FluentdUtil.PATRONI_LOG_TYPE + "\n"
        + "    Rule         $message ^.*$ "
        + tagName(cluster, FluentdUtil.PATRONI_LOG_TYPE)
        + "." + clusterNamespace + ".${HOSTNAME} false\n"
        + "    Emitter_Name patroni_re_emitted"
        + "\n"
        + "[FILTER]\n"
        + "    Name             kubernetes\n"
        + "    Match            " + tagName(cluster, "*") + "\n"
        + "    Annotations      Off\n"
        + "    Kube_Tag_Prefix  ''\n"
        + "    Regex_Parser     kubernetes_tag\n"
        + "    Buffer_Size      0\n"
        + "\n"
        + "[OUTPUT]\n"
        + "    Name              forward\n"
        + "    Match             " + tagName(cluster, "*") + "\n"
        + "    Host              " + fluentdServiceName + "." + fluentdNamespace + "\n"
        + "    Port              " + FluentdUtil.FORWARD_PORT + "\n"
        + "\n"
        + "[OUTPUT]\n"
        + "    Name              stdout\n"
        + "    Match             " + tagName(cluster, "*") + "\n"
        + "\n";
    Map<String, String> data = ImmutableMap.of(
        "parsers.conf", parsersConfigFile,
        "fluentbit.conf", fluentBitConfigFile);

    ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configName(context))
        .withLabels(labelFactory.clusterLabels(cluster))
        .endMetadata()
        .withData(data)
        .build();

    return Seq.of(configMap);
  }

  @Override
  public ImmutableList<Volume> getVolumes(
      StackGresClusterContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build());
  }

}
