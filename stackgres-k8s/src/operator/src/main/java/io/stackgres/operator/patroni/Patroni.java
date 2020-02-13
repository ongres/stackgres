/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.TCPSocketActionBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.cluster.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.cluster.ClusterStatefulSetVolumeMounts;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operatorframework.resource.ResourceGenerator;

import org.jooq.lambda.Unchecked;

@Singleton
public class Patroni implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String NAME = "patroni";

  private static final String IMAGE_PREFIX = "docker.io/ongres/patroni:v%s-pg%s-build-%s";
  private static final String DEFAULT_VERSION = StackGresComponents.get("patroni");

  private final PatroniRequirements resourceRequirementsFactory;
  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final PatroniEnvironmentVariables patroniEnvironmentVariables;
  private final ClusterStatefulSetVolumeMounts volumeMountsFactory;
  private final PatroniConfigMap patroniConfigMap;
  private final PatroniSecret patroniSecret;
  private final PatroniRole patroniRole;
  private final PatroniServices patroniServices;
  private final PatroniConfigEndpoints patroniConfigEndpoints;

  @Inject
  public Patroni(PatroniConfigMap patroniConfigMap, PatroniSecret patroniSecret,
      PatroniRole patroniRole, PatroniServices patroniServices,
      PatroniConfigEndpoints patroniConfigEndpoints,
      PatroniRequirements resourceRequirementsFactory,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      PatroniEnvironmentVariables patroniEnvironmentVariables,
      ClusterStatefulSetVolumeMounts volumeMountsFactory) {
    super();
    this.patroniConfigMap = patroniConfigMap;
    this.patroniSecret = patroniSecret;
    this.patroniRole = patroniRole;
    this.patroniServices = patroniServices;
    this.patroniConfigEndpoints = patroniConfigEndpoints;
    this.resourceRequirementsFactory = resourceRequirementsFactory;
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
    this.volumeMountsFactory = volumeMountsFactory;
  }

  @Override
  public Container getContainer(StackGresGeneratorContext context) {
    StackGresClusterContext clusterContext = context.getClusterContext();
    final String pgVersion = StackGresComponents.calculatePostgresVersion(
        clusterContext.getCluster().getSpec().getPostgresVersion());

    ResourceRequirements podResources = resourceRequirementsFactory
        .createResource(clusterContext);

    return new ContainerBuilder()
      .withName(NAME)
      .withImage(String.format(IMAGE_PREFIX,
          DEFAULT_VERSION, pgVersion, StackGresUtil.CONTAINER_BUILD))
      .withCommand("/bin/sh", "-exc", Unchecked.supplier(() -> Resources
          .asCharSource(ClusterStatefulSet.class.getResource("/start-patroni.sh"),
              StandardCharsets.UTF_8)
          .read()).get())
      .withImagePullPolicy("Always")
      .withSecurityContext(new SecurityContextBuilder()
          .withRunAsUser(999L)
          .withAllowPrivilegeEscalation(Boolean.FALSE)
          .build())
      .withPorts(
          new ContainerPortBuilder()
              .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
              .withContainerPort(Envoy.PG_ENTRY_PORT).build(),
          new ContainerPortBuilder()
              .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
              .withContainerPort(Envoy.PG_RAW_ENTRY_PORT).build(),
          new ContainerPortBuilder().withContainerPort(8008).build())
      .withVolumeMounts(volumeMountsFactory.listResources(clusterContext))
      .withEnvFrom(new EnvFromSourceBuilder()
          .withConfigMapRef(new ConfigMapEnvSourceBuilder()
              .withName(PatroniConfigMap.name(clusterContext)).build())
          .build())
      .withEnv(ImmutableList.<EnvVar>builder()
          .addAll(clusterStatefulSetEnvironmentVariables.listResources(clusterContext))
          .addAll(patroniEnvironmentVariables.listResources(clusterContext))
          .build())
      .withLivenessProbe(new ProbeBuilder()
          .withTcpSocket(new TCPSocketActionBuilder()
              .withPort(new IntOrString(5432))
              .build())
          .withInitialDelaySeconds(15)
          .withPeriodSeconds(20)
          .withFailureThreshold(6)
          .build())
      .withReadinessProbe(new ProbeBuilder()
          .withHttpGet(new HTTPGetActionBuilder()
              .withPath("/health")
              .withPort(new IntOrString(8008))
              .withScheme("HTTP")
              .build())
          .withInitialDelaySeconds(5)
          .withPeriodSeconds(10)
          .build())
      .withResources(podResources)
      .build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(
      StackGresGeneratorContext context) {
    return ImmutableList.of();
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    return ResourceGenerator.with(context)
        .of(HasMetadata.class)
        .append(patroniConfigMap)
        .append(patroniSecret)
        .append(patroniRole)
        .append(patroniServices)
        .append(patroniConfigEndpoints)
        .stream();
  }

}
