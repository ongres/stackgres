/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.fluentbit;

import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetPath;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.distributedlogs.fluentd.Fluentd;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Sidecar("fluent-bit")
@Singleton
public class FluentBit implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String NAME = "fluent-bit";
  public static final String IMAGE_NAME = "docker.io/bitnami/fluent-bit:%s";

  private static final String CONFIG_SUFFIX = "-fluent-bit";

  public FluentBit() {
  }

  @Override
  public Container getContainer(StackGresGeneratorContext context) {
    return new ContainerBuilder()
        .withName(NAME)
        .withImage(String.format(IMAGE_NAME, StackGresComponents.get(NAME)))
        .withImagePullPolicy("Always")
        .withSecurityContext(new SecurityContextBuilder()
            .withRunAsUser(0L)
            .build())
        .withStdin(Boolean.TRUE)
        .withTty(Boolean.TRUE)
        .withCommand("/bin/sh", "-exc")
        .withArgs(
            "groupadd postgres -g 999\n"
            + "useradd postgres -u 999 -g 999\n"
            + "cat << 'EOF' | su postgres -c sh\n"
            + "export PATRONI_PID=\"$$(until ps -e -o pid,args \\\n"
            + "  | grep \"[/]usr/bin/patroni\" \\\n"
            + "  | sh -ec 'read PID ARGS; [ -n \"$PID\" ]; echo \"$PID\"'\n"
            + "do sleep 1; done)\"\n"
            + "/opt/bitnami/fluent-bit/bin/fluent-bit \\\n"
            + "  -c /etc/fluent-bit/fluentbit-input-tail.conf \\\n"
            + "  -o /etc/fluent-bit/fluentbit-output-forward.conf \\\n"
            + "  -o /etc/fluent-bit/fluentbit-output-stdout.conf\n"
            + "EOF")
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
    final String namespace = context.getClusterContext().getCluster().getMetadata().getNamespace();
    final String fluentdRelativeId = context.getClusterContext().getCluster().getSpec()
        .getDistributedLogs().getDistributedLogs();
    final String fluentdNamespace =
        StackGresUtil.getNamespaceFromRelativeId(fluentdRelativeId, namespace);
    final String fluentdServiceName = Fluentd.serviceName(
        StackGresUtil.getNameFromRelativeId(fluentdRelativeId));

    String parsersConfigFile = ""
        + "[PARSER]\n"
        + "    Name        pgcsvlog_first\n"
        + "    Format      regex\n"
        + "    Regex       "
          + "^(?<log_time>\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}.\\d*\\s\\S{3})"
          + ",(?<message>.*)\n"
        + "\n"
        + "[PARSER]\n"
        + "    Name        pgcsvlog_1\n"
        + "    Format      regex\n"
        + "    Regex       "
          + "^(?<log_time>\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2}.\\d*\\s\\S{3})"
          + ",(?<message>\"([^\"]*(?:\"\"[^\"]*)*)\"|)\n"
        + "\n";
    String inputTailConfigFile = ""
        + "[SERVICE]\n"
        + "    Parsers_File      /etc/fluent-bit/parsers.conf\n"
        + "\n"
        + "[INPUT]\n"
        + "    Name              tail\n"
        + "    Path              "
          + "/proc/${PATRONI_PID}/root/" + ClusterStatefulSetPath.PG_LOG_PATH.path() + "/*.csv\n"
        + "    DB                /tmp/pg-csv-logs.db\n"
        + "    Multiline         On\n"
        + "    Parser_Firstline  pgcsvlog_first\n"
        + "    Parser_1          pgcsvlog_1\n"
        + "    Tag               "
          + tagName(context.getClusterContext().getCluster(), "postgres") + "\n"
        + "\n";
    String outputForwardConfigFile = ""
        + "[OUTPUT]\n"
        + "    Name              forward\n"
        + "    Host              " + fluentdServiceName + "." + fluentdNamespace + "\n"
        + "    Port              " + Fluentd.FORWARD_PORT + "\n"
        + "    Match             *\n"
        + "\n";
    String outputStdoutConfigFile = ""
        + "[OUTPUT]\n"
        + "    Name              stdout\n"
        + "    Match             *\n"
        + "\n";
    Map<String, String> data = ImmutableMap.of(
        "parsers.conf", parsersConfigFile,
        "fluentbit-input-tail.conf", inputTailConfigFile,
        "fluentbit-output-forward.conf", outputForwardConfigFile,
        "fluentbit-output-stdout.conf", outputStdoutConfigFile);

    ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configName(context.getClusterContext()))
        .withLabels(context.getClusterContext().clusterLabels())
        .withOwnerReferences(context.getClusterContext().ownerReference())
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
