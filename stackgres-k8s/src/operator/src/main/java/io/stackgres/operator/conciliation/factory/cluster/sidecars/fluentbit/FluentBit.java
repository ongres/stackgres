/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.fluentbit;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.LogVolumeMounts;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsCluster;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsFlunetdConfigMap;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsService;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sidecar(StackGresContainer.FLUENT_BIT)
@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainer.FLUENT_BIT)
public class FluentBit implements
    ContainerFactory<ClusterContainerContext>,
    VolumeFactory<StackGresClusterContext> {

  private static final Logger FLEUNTBIT_LOGGER = LoggerFactory.getLogger("io.stackgres.fluent-bit");
  private static final String CONFIG_SUFFIX = "-fluent-bit";

  private final LabelFactoryForCluster labelFactory;

  private final LogVolumeMounts logMounts;
  private final PostgresSocketMounts postgresSocket;
  private final TemplatesMounts scriptTemplatesVolumeMounts;

  @Inject
  public FluentBit(LabelFactoryForCluster labelFactory,
      LogVolumeMounts logMounts,
      PostgresSocketMounts postgresSocket,
      TemplatesMounts scriptTemplatesVolumeMounts) {
    this.labelFactory = labelFactory;
    this.logMounts = logMounts;
    this.postgresSocket = postgresSocket;
    this.scriptTemplatesVolumeMounts = scriptTemplatesVolumeMounts;
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
  public boolean isActivated(ClusterContainerContext context) {
    return context.getClusterContext().getSource().getSpec().getDistributedLogs() != null;
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of(
        StackGresContext.FLUENTBIT_VERSION_KEY,
        StackGresComponent.FLUENT_BIT.get(context.getClusterContext().getCluster())
        .getLatestVersion());
  }

  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresContainer.FLUENT_BIT.getName())
        .withImage(StackGresComponent.FLUENT_BIT.get(context.getClusterContext().getCluster())
            .getLatestImageName())
        .withImagePullPolicy(getDefaultPullPolicy())
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh", "-ex",
            ClusterPath.TEMPLATES_PATH.path()
                + "/" + ClusterPath.LOCAL_BIN_START_FLUENTBIT_SH_PATH.filename())
        .addToEnv(
            new EnvVarBuilder()
            .withName(ClusterPath.FLUENT_BIT_LAST_CONFIG_PATH.name())
            .withValue(ClusterPath.FLUENT_BIT_LAST_CONFIG_PATH.path())
            .build())
        .addAllToEnv(logMounts.getDerivedEnvVars(context))
        .addAllToEnv(postgresSocket.getDerivedEnvVars(context))
        .addAllToEnv(scriptTemplatesVolumeMounts.getDerivedEnvVars(context))
        .addAllToVolumeMounts(logMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(postgresSocket.getVolumeMounts(context))
        .addAllToVolumeMounts(scriptTemplatesVolumeMounts.getVolumeMounts(context))
        .addToVolumeMounts(new VolumeMountBuilder()
            .withName(StackGresVolume.FLUENT_BIT.getName())
            .withMountPath("/etc/fluent-bit")
            .withReadOnly(Boolean.TRUE)
            .build())
        .build();
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(ImmutableVolumePair.builder()
        .volume(buildConfiMapVolume(context))
        .source(buildConfiMapSource(context))
        .build());
  }

  private Volume buildConfiMapVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.FLUENT_BIT.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build();
  }

  private Optional<HasMetadata> buildConfiMapSource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    if (cluster.getSpec().getDistributedLogs() == null
        || cluster.getSpec().getDistributedLogs().getSgDistributedLogs() == null) {
      return Optional.empty();
    }
    final String namespace = cluster.getMetadata().getNamespace();
    final String fluentdRelativeId = cluster.getSpec()
        .getDistributedLogs().getSgDistributedLogs();
    final String fluentdNamespace =
        StackGresUtil.getNamespaceFromRelativeId(fluentdRelativeId, namespace);
    final String fluentdServiceName = DistributedLogsService.serviceName(
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
    final String clusterNamespace = cluster.getMetadata().getNamespace();
    String fluentBitConfigFile = ""
        + "[SERVICE]\n"
        + "    Parsers_File      /etc/fluent-bit/parsers.conf\n"
        + "    Log_Level         " + (FLEUNTBIT_LOGGER.isTraceEnabled() ? "debug" : "info")
        + "\n"
        + "[INPUT]\n"
        + "    Name              tail\n"
        + "    Path              " + ClusterPath.PG_LOG_PATH.path() + "/postgres*.csv\n"
        + "    Tag               " + DistributedLogsFlunetdConfigMap.POSTGRES_LOG_TYPE + "\n"
        + "    DB                " + ClusterPath.PG_LOG_PATH.path() + "/postgreslog.db\n"
        + "    Multiline         On\n"
        + "    Parser_Firstline  postgreslog_firstline\n"
        + "    Parser_1          postgreslog_1\n"
        + "    Buffer_Max_Size   2M\n"
        + "    Skip_Long_Lines   On\n"
        + "\n"
        + "[INPUT]\n"
        + "    Name              tail\n"
        + "    Key               message\n"
        + "    Path              " + ClusterPath.PG_LOG_PATH.path() + "/patroni*.log\n"
        + "    Tag               " + DistributedLogsFlunetdConfigMap.PATRONI_LOG_TYPE + "\n"
        + "    DB                " + ClusterPath.PG_LOG_PATH.path() + "/patronilog.db\n"
        + "    Multiline         On\n"
        + "    Parser_Firstline  patronilog_firstline\n"
        + "    Parser_1          patronilog_1\n"
        + "    Buffer_Max_Size   2M\n"
        + "    Skip_Long_Lines   On\n"
        + "\n"
        + "[FILTER]\n"
        + "    Name         rewrite_tag\n"
        + "    Match        " + DistributedLogsFlunetdConfigMap.POSTGRES_LOG_TYPE + "\n"
        + "    Rule         $message ^.*$ "
        + tagName(cluster, DistributedLogsFlunetdConfigMap.POSTGRES_LOG_TYPE)
        + "." + clusterNamespace + ".${HOSTNAME} " + (FLEUNTBIT_LOGGER.isDebugEnabled() ? "true" : "false") + "\n"
        + "    Emitter_Name postgres_re_emitted"
        + "\n"
        + "[FILTER]\n"
        + "    Name         rewrite_tag\n"
        + "    Match        " + DistributedLogsFlunetdConfigMap.PATRONI_LOG_TYPE + "\n"
        + "    Rule         $message ^.*$ "
        + tagName(cluster, DistributedLogsFlunetdConfigMap.PATRONI_LOG_TYPE)
        + "." + clusterNamespace + ".${HOSTNAME} " + (FLEUNTBIT_LOGGER.isDebugEnabled() ? "true" : "false") + "\n"
        + "    Emitter_Name patroni_re_emitted"
        + "\n"
        + "[FILTER]\n"
        + "    Name                 kubernetes\n"
        + "    Match                " + tagName(cluster, "*") + "\n"
        + "    Annotations          Off\n"
        + "    Kube_Tag_Prefix      ''\n"
        + "    Regex_Parser         kubernetes_tag\n"
        + "    Buffer_Size          0\n"
        + "    Kube_Meta_Cache_TTL  60\n"
        + "\n"
        + "[OUTPUT]\n"
        + "    Name              forward\n"
        + "    Match             " + tagName(cluster, "*") + "\n"
        + "    Host              " + fluentdServiceName + "." + fluentdNamespace + "\n"
        + "    Port              " + DistributedLogsCluster.FORWARD_PORT + "\n"
        + "\n"
        + "[OUTPUT]\n"
        + "    Name              stdout\n"
        + "    Match             " + DistributedLogsFlunetdConfigMap.PATRONI_LOG_TYPE + "\n"
        + "\n"
        + "[OUTPUT]\n"
        + "    Name              stdout\n"
        + "    Match             " + DistributedLogsFlunetdConfigMap.POSTGRES_LOG_TYPE + "\n"
        + "\n";
    Map<String, String> data = Map.of(
        "parsers.conf", parsersConfigFile,
        "fluentbit.conf", fluentBitConfigFile);

    ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configName(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(data)
        .build();

    return Optional.of(configMap);
  }
}
