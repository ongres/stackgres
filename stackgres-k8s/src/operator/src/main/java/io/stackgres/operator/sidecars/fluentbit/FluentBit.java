/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.fluentbit;

import java.util.Map;
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
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetPath;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.distributedlogs.fluentd.Fluentd;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Sidecar(FluentBit.NAME)
@Singleton
public class FluentBit implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String NAME = StackgresClusterContainers.FLUENT_BIT;
  public static final String IMAGE_NAME = "docker.io/ongres/fluentbit:v%s-build-%s";
  private static final String DEFAULT_VERSION = StackGresComponents.get("fluentbit");

  private static final String CONFIG_SUFFIX = "-fluent-bit";

  private final LabelFactoryDelegator factoryDelegator;

  @Inject
  public FluentBit(LabelFactoryDelegator factoryDelegator) {
    this.factoryDelegator = factoryDelegator;
  }

  @Override
  public Container getContainer(StackGresGeneratorContext context) {
    return new ContainerBuilder()
        .withName(NAME)
        .withImage(String.format(IMAGE_NAME, DEFAULT_VERSION, StackGresContext.CONTAINER_BUILD))
        .withImagePullPolicy("IfNotPresent")
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh", "-exc")
        .withArgs(""
            + "CONFIG_PATH=/etc/fluent-bit\n"
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
            + "run_fluentbit() {\n"
            + "  set -x\n"
            + "  export PATRONI_PID=\"$$(until sh -xc '\n"
            + "  for PID in $(ls -1 /proc | grep \"^[0-9]\\+$\")\n"
            + "  do\n"
            + "    cat /proc/$PID/cmdline | tr \"\\0\" \" \" \\\n"
            + "      | grep -v \"^$\" | grep -q \"[/]usr/bin/patroni\" \\\n"
            + "      && echo \"$PID\" && exit\n"
            + "  done\n"
            + "  exit 1'\n"
            + "  do sleep 1; done)\"\n"
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
        .withVolumeMounts(
            new VolumeMountBuilder()
            .withName(NAME)
            .withMountPath("/etc/fluent-bit")
            .withReadOnly(Boolean.TRUE)
            .build())
        .build();
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + CONFIG_SUFFIX);
  }

  public static String tagName(StackGresCluster cluster, String suffix) {
    final String name = cluster.getMetadata().getName();
    final String namespace = cluster.getMetadata().getNamespace();
    return namespace + "." + name + "." + suffix;
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();
    final String namespace = cluster.getMetadata().getNamespace();
    final String fluentdRelativeId = cluster.getSpec()
        .getDistributedLogs().getDistributedLogs();
    final String fluentdNamespace =
        StackGresUtil.getNamespaceFromRelativeId(fluentdRelativeId, namespace);
    final String fluentdServiceName = Fluentd.serviceName(
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
        + "    Name        patronilog\n"
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
    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    final String clusterNamespace = labelFactory.clusterNamespace(cluster);
    String fluentBitConfigFile = ""
        + "[SERVICE]\n"
        + "    Parsers_File      /etc/fluent-bit/parsers.conf\n"
        + "\n"
        + "[INPUT]\n"
        + "    Name              tail\n"
        + "    Path              "
          + "/proc/${PATRONI_PID}/root/"
          + ClusterStatefulSetPath.PG_LOG_PATH.path() + "/postgres*.csv\n"
        + "    Tag               " + FluentdUtil.POSTGRES_LOG_TYPE + "\n"
        + "    DB                /tmp/postgreslog.db\n"
        + "    Multiline         On\n"
        + "    Parser_Firstline  postgreslog_firstline\n"
        + "    Parser_1          postgreslog_1\n"
        + "\n"
        + "[INPUT]\n"
        + "    Name              tail\n"
        + "    Key               message\n"
        + "    Path              "
          + "/proc/${PATRONI_PID}/root/"
          + ClusterStatefulSetPath.PG_LOG_PATH.path() + "/patroni*.log\n"
        + "    Tag               " + FluentdUtil.PATRONI_LOG_TYPE + "\n"
        + "    DB                /tmp/patronilog.db\n"
        + "    Parser            patronilog\n"
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
        .withName(configName(clusterContext))
        .withLabels(labelFactory.clusterLabels(cluster))
        .withOwnerReferences(clusterContext.getOwnerReferences())
        .endMetadata()
        .withData(data)
        .build();

    return Seq.of(configMap);
  }

  @Override
  public ImmutableList<Volume> getVolumes(
      StackGresGeneratorContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context.getClusterContext()))
            .build())
        .build());
  }

}
