/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresModules;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloper;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperContainerPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.DISTRIBUTEDLOGS_RECONCILIATION_CYCLE)
public class InitReconciliationCycle implements ContainerFactory<DistributedLogsContainerContext> {

  private final ContainerUserOverrideMounts containerUserOverrideMounts;

  private final PostgresSocketMount postgresSocket;

  private final PostgresDataMounts postgresDataMounts;

  @Inject
  public InitReconciliationCycle(
      ContainerUserOverrideMounts containerUserOverrideMounts,
      PostgresSocketMount postgresSocket,
      PostgresDataMounts postgresDataMounts) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresSocket = postgresSocket;
    this.postgresDataMounts = postgresDataMounts;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    final ObjectMeta metadata = context
        .getDistributedLogsContext()
        .getSource()
        .getMetadata();
    return new ContainerBuilder()
        .withName(StackGresInitContainer.DISTRIBUTEDLOGS_RECONCILIATION_CYCLE.getName())
        .withImage(StackGresModules.DISTRIBUTEDLOGS_CONTROLLER.getImageName())
        .withEnv(
            new EnvVarBuilder()
            .withName("COMMAND")
            .withValue("run-reconciliation-cycle")
            .build(),
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME
                .getEnvironmentVariableName())
            .withValue(metadata.getName())
            .build(),
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE
                .getEnvironmentVariableName())
            .withValue(metadata.getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME
                .getEnvironmentVariableName())
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty
                .DISTRIBUTEDLOGS_CONTROLLER_EXTENSIONS_REPOSITORY_URLS
                .getEnvironmentVariableName())
            .withValue(OperatorProperty.EXTENSIONS_REPOSITORY_URLS
                .getString())
            .build(),
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty
                .DISTRIBUTEDLOGS_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES
                .getEnvironmentVariableName())
            .withValue(Boolean.FALSE.toString())
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
            .withName("APP_OPTS")
            .withValue(System.getenv("APP_OPTS"))
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
            .withName("MEMORY_REQUEST")
            .withNewValueFrom()
            .withNewResourceFieldRef()
            .withResource("requests.memory")
            .withDivisor(new Quantity("1"))
            .withContainerName(
                StackGresInitContainer.DISTRIBUTEDLOGS_RECONCILIATION_CYCLE.getName())
            .endResourceFieldRef()
            .endValueFrom()
            .build())
        .addAllToVolumeMounts(postgresSocket.getVolumeMounts(context))
        .addAllToVolumeMounts(postgresDataMounts.getVolumeMounts(context))
        .addToVolumeMounts(
            new VolumeMountBuilder()
                .withName(StackGresVolume.FLUENTD.getName())
                .withMountPath("/fluentd")
                .withReadOnly(Boolean.FALSE)
                .build(),
            new VolumeMountBuilder()
                .withName(StackGresVolume.FLUENTD_CONFIG.getName())
                .withMountPath("/etc/fluentd")
                .withReadOnly(Boolean.TRUE)
                .build()
        )
        .addAllToVolumeMounts(containerUserOverrideMounts.getVolumeMounts(context))
        .addAllToVolumeMounts(Optional.of(context.getDistributedLogsContext().getConfig().getSpec())
            .map(StackGresConfigSpec::getDeveloper)
            .map(StackGresConfigDeveloper::getPatches)
            .map(StackGresConfigDeveloperPatches::getDistributedlogsController)
            .map(StackGresConfigDeveloperContainerPatches::getVolumeMounts)
            .stream()
            .flatMap(List::stream)
            .map(VolumeMount.class::cast)
            .toList())
        .build();
  }

  @Override
  public Map<String, String> getComponentVersions(DistributedLogsContainerContext context) {
    return Map.of();
  }
}
