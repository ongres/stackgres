/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
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
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.operator.cluster.factory.ClusterStatefulSet;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeMounts;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@Singleton
public class Patroni implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String NAME = "patroni";
  public static final String POST_INIT_SUFFIX = "-post-init";

  private static final String IMAGE_NAME = "docker.io/ongres/patroni:v%s-pg%s-build-%s";
  private static final String DEFAULT_VERSION = StackGresComponents.get("patroni");

  private final PatroniRequirements resourceRequirementsFactory;
  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final PatroniEnvironmentVariables patroniEnvironmentVariables;
  private final ClusterStatefulSetVolumeMounts volumeMountsFactory;
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
                 ClusterStatefulSetVolumeMounts volumeMountsFactory,
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
    this.volumeMountsFactory = volumeMountsFactory;
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

    return new ContainerBuilder()
        .withName(NAME)
        .withImage(String.format(IMAGE_NAME,
            DEFAULT_VERSION, pgVersion, StackGresContext.CONTAINER_BUILD))
        .withCommand("/bin/sh", "-exc", Unchecked.supplier(() -> Resources
            .asCharSource(ClusterStatefulSet.class.getResource("/start-patroni.sh"),
                StandardCharsets.UTF_8)
            .read()).get())
        .withImagePullPolicy("Always")
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
        .withVolumeMounts(volumeMountsFactory.listResources(clusterContext))
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
                    .withSubPath(PatroniScriptsConfigMap.scriptName(
                        t.v2, t.v1.getName(), t.v1.getDatabase()))
                    .withReadOnly(true)
                    .build())
                .toArray(VolumeMount[]::new))
        .addToVolumeMounts(new VolumeMountBuilder()
            .withName("post-init")
            .withMountPath("/etc/patroni/post-init.sh")
            .withSubPath("post-init.sh")
            .withReadOnly(true)
            .build())
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
        .map(t -> new VolumeBuilder()
            .withName(PatroniScriptsConfigMap.name(clusterContext,
                t.v2, t.v1.getName(), t.v1.getDatabase()))
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(PatroniScriptsConfigMap.name(clusterContext,
                    t.v2, t.v1.getName(), t.v1.getDatabase()))
                .withOptional(false)
                .build())
            .build())
        .append(new VolumeBuilder()
            .withName("post-init")
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(postInitName(clusterContext))
                .withDefaultMode(0555) // NOPMD
                .build())
            .build())
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
        .append(c -> {
          final StackGresClusterContext clusterContext = context.getClusterContext();
          final StackGresCluster cluster = clusterContext.getCluster();
          final LabelFactory<?> labelFactory = factoryDelegator
              .pickFactory(clusterContext);
          final Map<String, String> labels = labelFactory
              .clusterLabels(cluster);
          return Seq.of(new ConfigMapBuilder()
              .withNewMetadata()
              .withNamespace(labelFactory.clusterNamespace(cluster))
              .withName(postInitName(clusterContext))
              .withLabels(labels)
              .withOwnerReferences(clusterContext.getOwnerReferences())
              .endMetadata()
              .withData(ImmutableMap.of("post-init.sh",
                  Unchecked.supplier(() -> Resources
                      .asCharSource(ClusterStatefulSet.class.getResource("/post-init.sh"),
                          StandardCharsets.UTF_8)
                      .read()).get()
                      .replace("${POSTGRES_PORT}", String.valueOf(Envoy.PG_PORT))))
              .build());
        })
        .stream();
  }

}
