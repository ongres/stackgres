/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap.PATRONI_RESTAPI_PORT_NAME;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.patroni.factory.PatroniScriptsConfigMap;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@RunningContainer(order = 0)
public class Patroni implements ContainerFactory<StackGresClusterContext> {

  public static final String POST_INIT_SUFFIX = "-post-init";

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables;

  private final ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory;
  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public Patroni(
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      ClusterEnvironmentVariablesFactoryDiscoverer<StackGresClusterContext>
          clusterEnvVarFactoryDiscoverer,
      ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables,
      ResourceFactory<StackGresClusterContext, ResourceRequirements> requirementsFactory,
      LabelFactory<StackGresCluster> labelFactory) {
    super();
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
    this.requirementsFactory = requirementsFactory;
    this.labelFactory = labelFactory;
  }

  public String postInitName(StackGresClusterContext clusterContext) {
    final StackGresCluster cluster = clusterContext.getSource();
    final String clusterName = labelFactory.clusterName(cluster);
    return clusterName + POST_INIT_SUFFIX;
  }

  @Override
  public List<Volume> getVolumes(StackGresClusterContext context) {
    var indexedScripts = context.getIndexedScripts();
    var inlineScripts = indexedScripts.stream()
        .filter(t -> t.v1.getScript() != null)
        .map(t -> new VolumeBuilder()
            .withName(PatroniScriptsConfigMap.name(context, t))
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(PatroniScriptsConfigMap.name(context, t))
                .withOptional(false)
                .build())
            .build()).collect(Collectors.toUnmodifiableList());
    var configMapScripts = indexedScripts.stream()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getConfigMapKeyRef() != null)
        .map(t -> new VolumeBuilder()
            .withName(PatroniScriptsConfigMap.name(context, t))
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(t.v1.getScriptFrom().getConfigMapKeyRef().getName())
                .withOptional(false)
                .build())
            .build()).collect(Collectors.toUnmodifiableList());

    var secretScripts = indexedScripts.stream()
        .filter(t -> t.v1.getScriptFrom() != null)
        .filter(t -> t.v1.getScriptFrom().getSecretKeyRef() != null)
        .map(t -> new VolumeBuilder()
            .withName(PatroniScriptsConfigMap.name(context, t))
            .withSecret(new SecretVolumeSourceBuilder()
                .withSecretName(t.v1.getScriptFrom().getSecretKeyRef().getName())
                .withOptional(false)
                .build())
            .build()).collect(Collectors.toUnmodifiableList());
    return ImmutableList.<Volume>builder()
        .addAll(inlineScripts)
        .addAll(configMapScripts)
        .addAll(secretScripts)
        .build();
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContext context) {
    return ImmutableMap.of(
        StackGresContext.POSTGRES_VERSION_KEY,
        StackGresComponent.POSTGRESQL.findVersion(
            context.getCluster().getSpec().getPostgresVersion()),
        StackGresContext.PATRONI_VERSION_KEY,
        StackGresComponent.PATRONI.findLatestVersion());
  }

  @Override
  public Container getContainer(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final String patroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            cluster.getSpec().getPostgresVersion()));

    ResourceRequirements podResources = requirementsFactory
        .createResource(context);

    final String startScript = context.getRestoreBackup().isPresent()
        ? "/start-patroni-with-restore.sh" : "/start-patroni.sh";
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.PATRONI)
        .withImage(patroniImageName)
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.LOCAL_BIN_PATH.path() + startScript)
        .withImagePullPolicy("IfNotPresent")
        .withPorts(
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withContainerPort(EnvoyUtil.PG_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withName(PATRONI_RESTAPI_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
                .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.volumeMounts(context,
            ClusterStatefulSetVolumeConfig.DATA,
            ClusterStatefulSetVolumeConfig.SOCKET,
            ClusterStatefulSetVolumeConfig.SHARED_MEMORY,
            ClusterStatefulSetVolumeConfig.USER,
            ClusterStatefulSetVolumeConfig.LOG,
            ClusterStatefulSetVolumeConfig.LOCAL_BIN,
            ClusterStatefulSetVolumeConfig.PATRONI_ENV,
            ClusterStatefulSetVolumeConfig.PATRONI_CONFIG,
            ClusterStatefulSetVolumeConfig.BACKUP_ENV,
            ClusterStatefulSetVolumeConfig.BACKUP_SECRET,
            ClusterStatefulSetVolumeConfig.RESTORE_ENV,
            ClusterStatefulSetVolumeConfig.RESTORE_SECRET)
            .toArray(VolumeMount[]::new))
        .addToVolumeMounts(
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB64_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_LIB64_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_LIB_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_BIN_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_BIN_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_SHARE_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_SHARE_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_BIN_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(
                    ClusterStatefulSetPath.PG_EXTRA_BIN_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_LIB64_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(
                    ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.path(context))))
        .addAllToVolumeMounts(Seq.seq(Optional.ofNullable(cluster.getStatus())
            .map(StackGresClusterStatus::getPodStatuses))
            .flatMap(List::stream)
            .map(Optional::of)
            .map(podStatus -> podStatus
                .map(StackGresClusterPodStatus::getInstalledPostgresExtensions))
            .flatMap(Seq::seq)
            .flatMap(List::stream)
            .map(Optional::of)
            .map(installedExtension -> installedExtension
                .map(StackGresClusterInstalledExtension::getExtraMounts))
            .flatMap(Seq::seq)
            .flatMap(List::stream)
            .grouped(Function.identity())
            .map(Tuple2::v1)
            .map(extraMount -> ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH) + extraMount)
                .withMountPath(extraMount)))
            .toList())
        .addToVolumeMounts(
            context.getIndexedScripts().stream()
                .map(t -> new VolumeMountBuilder()
                    .withName(PatroniScriptsConfigMap.name(context, t))
                    .withMountPath("/etc/patroni/init-script.d/"
                        + PatroniScriptsConfigMap.scriptName(t))
                    .withSubPath(t.v1.getScript() != null
                        ? PatroniScriptsConfigMap.scriptName(t)
                        : t.v1.getScriptFrom().getConfigMapKeyRef() != null
                            ? t.v1.getScriptFrom().getConfigMapKeyRef().getKey()
                            : t.v1.getScriptFrom().getSecretKeyRef().getKey())
                    .withReadOnly(true)
                    .build())
                .toArray(VolumeMount[]::new))
        .withEnvFrom(new EnvFromSourceBuilder()
            .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                .withName(PatroniConfigMap.name(context)).build())
            .build())
        .withEnv(ImmutableList.<EnvVar>builder()
            .add(new EnvVarBuilder()
                .withName("PATH")
                .withValue(Seq.of(
                    ClusterStatefulSetPath.PG_EXTRA_BIN_PATH.path(context),
                    ClusterStatefulSetPath.PG_BIN_PATH.path(context),
                    "/usr/local/sbin",
                    "/usr/local/bin",
                    "/usr/sbin",
                    "/usr/bin",
                    "/sbin",
                    "/bin")
                    .toString(":"))
                .build())
            .add(new EnvVarBuilder()
                .withName("LD_LIBRARY_PATH")
                .withValue(Seq.of(
                    ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.path(context))
                    .toString(":"))
                .build())
            .addAll(clusterStatefulSetEnvironmentVariables.listResources(cluster))
            .addAll(patroniEnvironmentVariables.createResource(context))
            .build())
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/cluster")
                .withPort(new IntOrString(EnvoyUtil.PATRONI_ENTRY_PORT))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(15)
            .withPeriodSeconds(20)
            .withFailureThreshold(6)
            .build())
        .withReadinessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/read-only")
                .withPort(new IntOrString(EnvoyUtil.PATRONI_ENTRY_PORT))
                .withScheme("HTTP")
                .build())
            .withInitialDelaySeconds(5)
            .withPeriodSeconds(10)
            .build())
        .withResources(podResources)
        .build();
  }

}
