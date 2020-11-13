/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.distributedlogs.controller;

import java.util.stream.Stream;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresDistributedLogsGeneratorContext;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.factory.ContainerResourceFactory;

@Singleton
public class DistributedLogsController implements ContainerResourceFactory<StackGresDistributedLogs,
    StackGresDistributedLogsGeneratorContext, StackGresDistributedLogs> {
  public static final String IMAGE_NAME = "docker.io/stackgres/distributedlogs-controller:%s";

  @Override
  public Container getContainer(StackGresDistributedLogsGeneratorContext context) {
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.DISTRIBUTEDLOGS_CONTROLLER)
        .withImage(String.format(IMAGE_NAME, StackGresProperty.OPERATOR_IMAGE_VERSION.getString()))
        .withImagePullPolicy("IfNotPresent")
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/health/live")
                .withPort(new IntOrString(8080))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(30)
            .withTimeoutSeconds(10)
            .build())
        .withReadinessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/health/ready")
                .withPort(new IntOrString(8080))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(30)
            .withTimeoutSeconds(2)
            .build())
        .withEnv(new EnvVarBuilder()
            .withName("DISTRIBUTEDLOGS_NAME")
            .withValue(context.getDistributedLogsContext()
                .getDistributedLogs().getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName("DISTRIBUTEDLOGS_NAMESPACE")
            .withValue(context.getDistributedLogsContext()
                .getDistributedLogs().getMetadata().getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName("DISTRIBUTEDLOGS_CONTROLLER_LOG_LEVEL")
            .withValue(System.getenv("OPERATOR_LOG_LEVEL"))
            .build(),
            new EnvVarBuilder()
            .withName("DISTRIBUTEDLOGS_CONTROLLER_SHOW_STACK_TRACES")
            .withValue(System.getenv("OPERATOR_SHOW_STACK_TRACES"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_DISTRIBUTEDLOGS_CONTROLLER")
            .withValue(System.getenv("DEBUG_OPERATOR"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_DISTRIBUTEDLOGS_CONTROLLER_SUSPEND")
            .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
            .build(),
            new EnvVarBuilder()
            .withName("DISTRIBUTEDLOGS_CONTROLLER_POD_NAME")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                .build())
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.SOCKET
            .volumeMount(context.getClusterContext()),
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

  public StackGresClusterSidecarResourceFactory<Void> toStackGresClusterSidecarResourceFactory() {
    return new DistributedLogsControllerStackGresClusterSidecarResourceFactory();
  }

  private class DistributedLogsControllerStackGresClusterSidecarResourceFactory
      implements StackGresClusterSidecarResourceFactory<Void> {

    @Override
    public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
      if (context instanceof StackGresDistributedLogsGeneratorContext) {
        return DistributedLogsController.this.streamResources(
            (StackGresDistributedLogsGeneratorContext) context);
      } else {
        throw new IllegalArgumentException(
            "context is not a StackGresDistributedLogsGeneratorContext");
      }
    }

    @Override
    public ImmutableList<Volume> getVolumes(StackGresGeneratorContext context) {
      if (context instanceof StackGresDistributedLogsGeneratorContext) {
        return DistributedLogsController.this.getVolumes(
            (StackGresDistributedLogsGeneratorContext) context);
      } else {
        throw new IllegalArgumentException(
            "context is not a StackGresDistributedLogsGeneratorContext");
      }
    }

    @Override
    public Container getContainer(StackGresGeneratorContext context) {
      if (context instanceof StackGresDistributedLogsGeneratorContext) {
        return DistributedLogsController.this.getContainer(
            (StackGresDistributedLogsGeneratorContext) context);
      } else {
        throw new IllegalArgumentException(
            "context is not a StackGresDistributedLogsGeneratorContext");
      }
    }
  }

}
