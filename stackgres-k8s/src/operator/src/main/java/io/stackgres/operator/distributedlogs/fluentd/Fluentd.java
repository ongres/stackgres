/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.distributedlogs.fluentd;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.TCPSocketActionBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresDistributedLogsGeneratorContext;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.sidecars.fluentbit.FluentBit;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.operatorframework.resource.factory.ContainerResourceFactory;
import org.jooq.lambda.Seq;

@Singleton
public class Fluentd implements ContainerResourceFactory<StackGresDistributedLogs,
    StackGresDistributedLogsGeneratorContext, StackGresDistributedLogs> {

  public static final String NAME = "fluentd";

  public static final int FORWARD_PORT = 12225;
  public static final String FORWARD_PORT_NAME = "forward";

  private static final String IMAGE_NAME = "fluent/fluentd:v%s";
  private static final String DEFAULT_VERSION = StackGresComponents.get("fluentd");

  private static final String SUFFIX = "-fluentd";

  public static String configName(StackGresDistributedLogs distributedLogs) {
    return ResourceUtil.resourceName(distributedLogs.getMetadata().getName() + SUFFIX);
  }

  public static String serviceName(StackGresDistributedLogs distributedLogs) {
    return serviceName(distributedLogs.getMetadata().getName());
  }

  public static String serviceName(String distributedLogsName) {
    return ResourceUtil.resourceName(distributedLogsName + SUFFIX);
  }

  @Override
  public Container getContainer(StackGresDistributedLogsGeneratorContext context) {
    return new ContainerBuilder()
      .withName(NAME)
      .withImage(String.format(IMAGE_NAME, DEFAULT_VERSION))
      .withCommand("/usr/bin/fluentd", "-c", "/etc/fluentd/fluentd.conf")
      .withImagePullPolicy("Always")
      .withSecurityContext(new SecurityContextBuilder()
          .withRunAsUser(999L)
          .withAllowPrivilegeEscalation(Boolean.FALSE)
          .build())
      .withPorts(
          new ContainerPortBuilder()
              .withName(FORWARD_PORT_NAME)
              .withContainerPort(FORWARD_PORT).build())
      .withLivenessProbe(new ProbeBuilder()
          .withTcpSocket(new TCPSocketActionBuilder()
              .withPort(new IntOrString(FORWARD_PORT))
              .build())
          .withInitialDelaySeconds(15)
          .withPeriodSeconds(20)
          .withFailureThreshold(6)
          .build())
      .withReadinessProbe(new ProbeBuilder()
          .withTcpSocket(new TCPSocketActionBuilder()
              .withPort(new IntOrString(FORWARD_PORT))
              .build())
          .withInitialDelaySeconds(5)
          .withPeriodSeconds(10)
          .build())
      .withVolumeMounts(
          new VolumeMountBuilder()
          .withName(NAME)
          .withMountPath("/etc/fluentd")
          .withReadOnly(Boolean.TRUE)
          .build())
      .build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(StackGresDistributedLogsGeneratorContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context.getDistributedLogsContext().getDistributedLogs()))
            .build())
        .build());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresDistributedLogsGeneratorContext context) {
    final StackGresDistributedLogs distributedLogs =
        context.getDistributedLogsContext().getDistributedLogs();
    final String namespace = distributedLogs.getMetadata().getNamespace();

    final String configFile = ""
            + "<source>\n"
            + "@type forward\n"
            + "bind 0.0.0.0\n"
            + "port " + FORWARD_PORT + "\n"
            + "</source>\n"
            + "\n"
            + context.getDistributedLogsContext().getConnectedClusters()
            .stream()
            .map(cluster -> FluentBit.tagName(cluster, "*"))
            .map(clusterTag -> ""
                + "<match " + clusterTag + ">\n"
                + "  @type stdout\n"
                + "</match>\n")
            .collect(Collectors.joining("\n"));
    final Map<String, String> data = ImmutableMap.of(
        "fluentd.conf", configFile);

    final ConfigMap configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configName(distributedLogs))
        .withLabels(context.getClusterContext().clusterLabels())
        .withOwnerReferences(context.getClusterContext().ownerReference())
        .endMetadata()
        .withData(data)
        .build();

    final Service service = new ServiceBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(serviceName(distributedLogs))
        .withLabels(context.getClusterContext().statefulSetPodLabels())
        .withOwnerReferences(context.getClusterContext().ownerReference())
        .endMetadata()
        .withNewSpec()
        .withClusterIP("None")
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

}
