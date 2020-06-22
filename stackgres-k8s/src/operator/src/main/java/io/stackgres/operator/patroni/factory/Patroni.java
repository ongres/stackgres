/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
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
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetPath;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import org.jooq.lambda.Seq;

@Singleton
public class Patroni implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String POST_INIT_SUFFIX = "-post-init";

  private static final String IMAGE_NAME = "docker.io/ongres/patroni:v%s-pg%s-build-%s";
  private static final String DEFAULT_VERSION = StackGresComponents.get("patroni");

  private final PatroniRequirements resourceRequirementsFactory;
  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final PatroniEnvironmentVariables patroniEnvironmentVariables;
  private final PatroniConfigMap patroniConfigMap;
  private final PatroniScriptsConfigMap patroniScriptsConfigMap;
  private final PatroniSecret patroniSecret;
  private final PatroniRole patroniRole;
  private final PatroniServices patroniServices;
  private final PatroniConfigEndpoints patroniConfigEndpoints;
  private final LabelFactoryDelegator factoryDelegator;

  @Inject
  public Patroni(PatroniConfigMap patroniConfigMap,
      PatroniScriptsConfigMap patroniScriptsConfigMap,
      PatroniSecret patroniSecret,
      PatroniRole patroniRole, PatroniServices patroniServices,
      PatroniConfigEndpoints patroniConfigEndpoints,
      PatroniRequirements resourceRequirementsFactory,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      PatroniEnvironmentVariables patroniEnvironmentVariables,
      LabelFactoryDelegator factoryDelegator) {
    super();
    this.patroniConfigMap = patroniConfigMap;
    this.patroniScriptsConfigMap = patroniScriptsConfigMap;
    this.patroniSecret = patroniSecret;
    this.patroniRole = patroniRole;
    this.patroniServices = patroniServices;
    this.patroniConfigEndpoints = patroniConfigEndpoints;
    this.resourceRequirementsFactory = resourceRequirementsFactory;
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
    this.factoryDelegator = factoryDelegator;
  }

  public String postInitName(StackGresClusterContext clusterContext) {
    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    final StackGresCluster cluster = clusterContext.getCluster();
    final String clusterName = labelFactory.clusterName(cluster);
    return clusterName + POST_INIT_SUFFIX;
  }

  @Override
  public Container getContainer(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();
    final String pgVersion = StackGresComponents.calculatePostgresVersion(
        cluster.getSpec().getPostgresVersion());

    ResourceRequirements podResources = resourceRequirementsFactory
        .createResource(clusterContext);

    final String startScript = context.getClusterContext().getRestoreContext().isPresent()
        ? "/start-patroni-with-restore.sh" : "/start-patroni.sh";
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.PATRONI)
        .withImage(String.format(IMAGE_NAME,
            DEFAULT_VERSION, pgVersion, StackGresContext.CONTAINER_BUILD))
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.LOCAL_BIN_PATH.path() + startScript)
        .withImagePullPolicy("IfNotPresent")
        .withPorts(
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withContainerPort(clusterContext.getSidecars().stream()
                    .filter(entry -> entry.getSidecar() instanceof Envoy)
                    .map(entry -> Envoy.PG_ENTRY_PORT)
                    .findFirst()
                    .orElse(Envoy.PG_PORT)).build(),
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withContainerPort(clusterContext.getSidecars().stream()
                    .filter(entry -> entry.getSidecar() instanceof Envoy)
                    .map(entry -> Envoy.PG_REPL_ENTRY_PORT)
                    .findFirst()
                    .orElse(Envoy.PG_PORT)).build(),
            new ContainerPortBuilder().withContainerPort(8008).build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.volumeMounts(context.getClusterContext(),
            ClusterStatefulSetVolumeConfig.DATA,
            ClusterStatefulSetVolumeConfig.SOCKET,
            ClusterStatefulSetVolumeConfig.LOCAL,
            ClusterStatefulSetVolumeConfig.PATRONI_ENV,
            ClusterStatefulSetVolumeConfig.PATRONI_CONFIG,
            ClusterStatefulSetVolumeConfig.BACKUP_ENV,
            ClusterStatefulSetVolumeConfig.BACKUP_SECRET,
            ClusterStatefulSetVolumeConfig.RESTORE_ENV,
            ClusterStatefulSetVolumeConfig.RESTORE_SECRET)
            .toArray(VolumeMount[]::new))
        .addToVolumeMounts(
            Seq.of(Optional.ofNullable(
                cluster.getSpec().getInitData())
                .map(StackGresClusterInitData::getScripts))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMap(List::stream)
                .zipWithIndex()
                .map(t -> new VolumeMountBuilder()
                    .withName(PatroniScriptsConfigMap.name(
                        clusterContext, t.v2, t.v1.getName(), t.v1.getDatabase()))
                    .withMountPath("/etc/patroni/init-script.d/"
                        + PatroniScriptsConfigMap.scriptName(
                        t.v2, t.v1.getName(), t.v1.getDatabase()))
                    .withSubPath(t.v1.getScript() != null
                        ? PatroniScriptsConfigMap.scriptName(
                            t.v2, t.v1.getName(), t.v1.getDatabase())
                        : t.v1.getScriptFrom().getConfigMapKeyRef() != null
                            ? t.v1.getScriptFrom().getConfigMapKeyRef().getKey()
                            : t.v1.getScriptFrom().getSecretKeyRef().getKey())
                    .withReadOnly(true)
                    .build())
                .toArray(VolumeMount[]::new))
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(PatroniConfigMap.name(clusterContext)).build())
            .build())
        .withEnv(ImmutableList.<EnvVar>builder()
            .addAll(clusterStatefulSetEnvironmentVariables.listResources(clusterContext))
            .addAll(patroniEnvironmentVariables.listResources(clusterContext))
            .build())
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withNewPath("/cluster")
                .withPort(new IntOrString(8008))
                .withScheme("HTTP")
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
  public ImmutableList<Volume> getVolumes(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    return Seq.of(Optional.ofNullable(
        clusterContext.getCluster().getSpec().getInitData())
        .map(StackGresClusterInitData::getScripts))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(List::stream)
        .zipWithIndex()
        .filter(t -> t.v1.getScript() != null)
        .map(t -> new VolumeBuilder()
            .withName(PatroniScriptsConfigMap.name(clusterContext,
                t.v2, t.v1.getName(), t.v1.getDatabase()))
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(PatroniScriptsConfigMap.name(clusterContext,
                    t.v2, t.v1.getName(), t.v1.getDatabase()))
                .withOptional(false)
                .build())
            .build())
        .append(Seq.of(Optional.ofNullable(
            clusterContext.getCluster().getSpec().getInitData())
            .map(StackGresClusterInitData::getScripts))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .zipWithIndex()
            .filter(t -> t.v1.getScriptFrom() != null)
            .filter(t -> t.v1.getScriptFrom().getConfigMapKeyRef() != null)
            .map(t -> new VolumeBuilder()
                .withName(PatroniScriptsConfigMap.name(clusterContext,
                    t.v2, t.v1.getName(), t.v1.getDatabase()))
                .withConfigMap(new ConfigMapVolumeSourceBuilder()
                    .withName(t.v1.getScriptFrom().getConfigMapKeyRef().getName())
                    .withOptional(false)
                    .build())
                .build()))
        .append(Seq.of(Optional.ofNullable(
            clusterContext.getCluster().getSpec().getInitData())
            .map(StackGresClusterInitData::getScripts))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .zipWithIndex()
            .filter(t -> t.v1.getScriptFrom() != null)
            .filter(t -> t.v1.getScriptFrom().getSecretKeyRef() != null)
            .map(t -> new VolumeBuilder()
                .withName(PatroniScriptsConfigMap.name(clusterContext,
                    t.v2, t.v1.getName(), t.v1.getDatabase()))
                .withSecret(new SecretVolumeSourceBuilder()
                    .withSecretName(t.v1.getScriptFrom().getSecretKeyRef().getName())
                    .withOptional(false)
                    .build())
                .build()))
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    return ResourceGenerator.with(context)
        .of(HasMetadata.class)
        .append(patroniConfigMap)
        .append(patroniScriptsConfigMap)
        .append(patroniSecret)
        .append(patroniRole)
        .append(patroniServices)
        .append(patroniConfigEndpoints)
        .stream();
  }

}
