/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.fluentbit;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.VolumeMountProviderName;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ClusterRunningContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;

@Sidecar(AbstractFluentBit.NAME)
@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V11)
@RunningContainer(ClusterRunningContainer.FLUENT_BIT)
public class FluentBit extends AbstractFluentBit {

  private final VolumeMountsProvider<ContainerContext> logMounts;

  @Inject
  public FluentBit(LabelFactoryForCluster<StackGresCluster> labelFactory,
                   @ProviderName(VolumeMountProviderName.POSTGRES_LOG)
                       VolumeMountsProvider<ContainerContext> logMounts) {
    super(labelFactory);
    this.logMounts = logMounts;
  }

  @Override
  protected List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context) {
    return ImmutableList.<VolumeMount>builder()
        .addAll(logMounts.getVolumeMounts(context))
        .add(
            new VolumeMountBuilder()
                .withName(StatefulSetDynamicVolumes.FLUENT_BIT.getVolumeName())
                .withMountPath("/etc/fluent-bit")
                .withReadOnly(Boolean.TRUE)
                .build())
        .build();
  }

  @Override
  protected List<EnvVar> getContainerEnvironmentVariables(
      StackGresClusterContainerContext context) {
    return logMounts.getDerivedEnvVars(context);
  }

  protected Optional<HasMetadata> buildSource(StackGresClusterContext context) {
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
    final String clusterNamespace = cluster.getMetadata().getNamespace();
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
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(data)
        .build();

    return Optional.of(configMap);
  }
}
