/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.controller;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresController;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloper;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperContainerPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeveloperPatches;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;

@Singleton
@OperatorVersionBinder
@RunningContainer(StackGresContainer.DISTRIBUTEDLOGS_CONTROLLER)
public class DistributedLogsController
    implements ContainerFactory<DistributedLogsContainerContext> {

  private final ContainerUserOverrideMounts containerUserOverrideMounts;

  private final PostgresSocketMount postgresSocket;

  private final PostgresDataMounts postgresDataMounts;

  @Inject
  public DistributedLogsController(
      PostgresDataMounts postgresDataMounts,
      ContainerUserOverrideMounts containerUserOverrideMounts,
      PostgresSocketMount postgresSocket) {
    this.containerUserOverrideMounts = containerUserOverrideMounts;
    this.postgresSocket = postgresSocket;
    this.postgresDataMounts = postgresDataMounts;
  }

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresContainer.DISTRIBUTEDLOGS_CONTROLLER.getName())
        .withImage(StackGresController.DISTRIBUTEDLOGS_CONTROLLER.getImageName())
        .withImagePullPolicy(getDefaultPullPolicy())
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/q/health/live")
                .withPort(new IntOrString(8080))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(30)
            .withTimeoutSeconds(10)
            .build())
        .withReadinessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/q/health/ready")
                .withPort(new IntOrString(8080))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(30)
            .withTimeoutSeconds(2)
            .build())
        .withEnv(
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAME
                .getEnvironmentVariableName())
            .withValue(context
                .getDistributedLogsContext()
                .getSource().getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName(DistributedLogsControllerProperty.DISTRIBUTEDLOGS_NAMESPACE
                .getEnvironmentVariableName())
            .withValue(context
                .getDistributedLogsContext()
                .getSource().getMetadata().getNamespace())
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
            .withName("JAVA_OPTS")
            .withValue(System.getenv("JAVA_OPTS"))
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
            .withContainerName(StackGresContainer.DISTRIBUTEDLOGS_CONTROLLER.getName())
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
    return ImmutableMap.of(
        StackGresContext.DISTRIBUTEDLOGS_CONTROLLER_VERSION_KEY,
        StackGresController.DISTRIBUTEDLOGS_CONTROLLER.getVersion());
  }

}
