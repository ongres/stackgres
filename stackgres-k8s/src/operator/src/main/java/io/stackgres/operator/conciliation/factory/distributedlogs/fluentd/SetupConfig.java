/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.fluentd;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;

@InitContainer(order = 4)
@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V095)
public class SetupConfig implements ContainerFactory<DistributedLogsContext> {

  @Override
  public Container getContainer(DistributedLogsContext context) {
    return new ContainerBuilder()
        .withName("setup-fluentd-config")
        .withImage(StackGresComponent.KUBECTL.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ecx", Stream.of(
            "cp /etc/fluentd/initial-fluentd.conf /fluentd/fluentd.conf")
            .collect(Collectors.joining(" && ")))
        .withVolumeMounts(
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

  @Override
  public List<Volume> getVolumes(DistributedLogsContext context) {
    return List.of();
  }

  @Override
  public Map<String, String> getComponentVersions(DistributedLogsContext context) {
    return Map.of();
  }
}
