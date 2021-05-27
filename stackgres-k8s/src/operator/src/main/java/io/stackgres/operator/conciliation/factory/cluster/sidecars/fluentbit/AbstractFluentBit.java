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
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
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

  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public AbstractFluentBit(
      LabelFactory<StackGresCluster> labelFactory) {
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
        StackGresComponent.FLUENT_BIT.findLatestVersion());
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(NAME)
        .withImage(getImageImageName())
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
        .withEnv(getContainerEnvironmentVariables(context))
        .withVolumeMounts(getVolumeMounts(context))
        .build();
  }

  protected String getImageImageName() {
    return StackGresComponent.FLUENT_BIT.findLatestImageName();
  }

  public abstract List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context);

  protected abstract List<EnvVar> getContainerEnvironmentVariables(
      StackGresClusterContainerContext context);

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(ImmutableVolumePair.builder()
        .volume(buildVolume(context))
        .source(buildSource(context))
        .build());
  }

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StatefulSetDynamicVolumes.FLUENT_BIT.getVolumeName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build();
  }

  public @NotNull Optional<HasMetadata> buildSource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    if (cluster.getSpec().getDistributedLogs() == null) {
      return Optional.empty();
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

    return Optional.of(configMap);

  }

}
