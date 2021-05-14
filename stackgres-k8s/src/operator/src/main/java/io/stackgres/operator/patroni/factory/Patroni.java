/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import static io.stackgres.operator.patroni.factory.PatroniConfigMap.PATRONI_RESTAPI_PORT_NAME;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

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
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operatorframework.resource.ResourceGenerator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
public class Patroni implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String POST_INIT_SUFFIX = "-post-init";

  private final PatroniRequirements resourceRequirementsFactory;
  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final PatroniEnvironmentVariables patroniEnvironmentVariables;
  private final PatroniConfigMap patroniConfigMap;
  private final PatroniScriptsConfigMap patroniScriptsConfigMap;
  private final PatroniSecret patroniSecret;
  private final PatroniRole patroniRole;
  private final PatroniServices patroniServices;
  private final PatroniEndpoints patroniConfigEndpoints;
  private final LabelFactoryDelegator factoryDelegator;

  @Inject
  public Patroni(PatroniConfigMap patroniConfigMap,
      PatroniScriptsConfigMap patroniScriptsConfigMap,
      PatroniSecret patroniSecret,
      PatroniRole patroniRole, PatroniServices patroniServices,
      PatroniEndpoints patroniConfigEndpoints,
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
  public Container getContainer(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    final String patroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            cluster.getSpec().getPostgresVersion()));

    ResourceRequirements podResources = resourceRequirementsFactory
        .createResource(context);
    final String startScript = context.getRestoreContext().isPresent()
        ? "/start-patroni-with-restore.sh"
        : "/start-patroni.sh";
    return new ContainerBuilder()
        .withName(StackgresClusterContainers.PATRONI)
        .withImage(patroniImageName)
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.LOCAL_BIN_PATH.path() + startScript)
        .withImagePullPolicy("IfNotPresent")
        .withPorts(
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_PORT_NAME)
                .withContainerPort(context.getSidecars().stream()
                    .filter(entry -> entry.getSidecar() instanceof Envoy)
                    .map(entry -> EnvoyUtil.PG_ENTRY_PORT)
                    .findFirst()
                    .orElse(EnvoyUtil.PG_PORT))
                .build(),
            new ContainerPortBuilder()
                .withName(PatroniConfigMap.POSTGRES_REPLICATION_PORT_NAME)
                .withContainerPort(context.getSidecars().stream()
                    .filter(entry -> entry.getSidecar() instanceof Envoy)
                    .map(entry -> EnvoyUtil.PG_REPL_ENTRY_PORT)
                    .findFirst()
                    .orElse(EnvoyUtil.PG_PORT))
                .build(),
            new ContainerPortBuilder()
                .withName(PATRONI_RESTAPI_PORT_NAME)
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
                .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.allVolumeMounts(context,
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
                    .withSubPath(
                        ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.subPath(context,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                    .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                    .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_BIN_PATH.subPath(context,
                        ClusterStatefulSetPath.PG_BASE_PATH))
                    .withMountPath(
                        ClusterStatefulSetPath.PG_EXTENSIONS_MOUNTED_BIN_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                    .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_LIB64_PATH.subPath(context,
                        ClusterStatefulSetPath.PG_BASE_PATH))
                    .withMountPath(
                        ClusterStatefulSetPath.PG_EXTENSIONS_MOUNTED_LIB64_PATH.path(context))))
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
                    .withSubPath(
                        ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.subPath(context,
                            ClusterStatefulSetPath.PG_BASE_PATH) + extraMount)
                    .withMountPath(extraMount)))
            .toList())
        .addToVolumeMounts(
            context.getIndexedScripts()
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
                    ClusterStatefulSetPath.PG_EXTENSIONS_MOUNTED_BIN_PATH.path(context),
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
                    ClusterStatefulSetPath.PG_EXTENSIONS_MOUNTED_LIB64_PATH.path(context))
                    .toString(":"))
                .build())
            .addAll(clusterStatefulSetEnvironmentVariables.listResources(context))
            .addAll(patroniEnvironmentVariables.listResources(context))
            .build())
        .withLivenessProbe(new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withNewPath("/cluster")
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

  @Override
  public ImmutableList<Volume> getVolumes(StackGresClusterContext context) {
    return context.getIndexedScripts()
        .filter(t -> t.v1.getScript() != null)
        .map(t -> new VolumeBuilder()
            .withName(PatroniScriptsConfigMap.name(context, t))
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(PatroniScriptsConfigMap.name(context, t))
                .withOptional(false)
                .build())
            .build())
        .append(context.getIndexedScripts()
            .filter(t -> t.v1.getScriptFrom() != null)
            .filter(t -> t.v1.getScriptFrom().getConfigMapKeyRef() != null)
            .map(t -> new VolumeBuilder()
                .withName(PatroniScriptsConfigMap.name(context, t))
                .withConfigMap(new ConfigMapVolumeSourceBuilder()
                    .withName(t.v1.getScriptFrom().getConfigMapKeyRef().getName())
                    .withOptional(false)
                    .build())
                .build()))
        .append(context.getIndexedScripts()
            .filter(t -> t.v1.getScriptFrom() != null)
            .filter(t -> t.v1.getScriptFrom().getSecretKeyRef() != null)
            .map(t -> new VolumeBuilder()
                .withName(PatroniScriptsConfigMap.name(context, t))
                .withSecret(new SecretVolumeSourceBuilder()
                    .withSecretName(t.v1.getScriptFrom().getSecretKeyRef().getName())
                    .withOptional(false)
                    .build())
                .build()))
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresClusterContext context) {
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

  @Override
  public Stream<Container> getInitContainers(StackGresClusterContext context) {
    return setupMajorVersionUpgrade(context);
  }

  private Stream<Container> setupMajorVersionUpgrade(StackGresClusterContext context) {
    if (!Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .isPresent()) {
      return Stream.of();
    }
    StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgradeStatus =
        Optional.of(context.getCluster())
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getDbOps)
            .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
            .get();
    String primaryInstance = majorVersionUpgradeStatus.getPrimaryInstance();
    String targetVersion = majorVersionUpgradeStatus.getTargetPostgresVersion();
    String sourceVersion = majorVersionUpgradeStatus.getSourcePostgresVersion();
    String sourceMajorVersion = StackGresComponent.POSTGRESQL.findMajorVersion(sourceVersion);
    ImmutableMap<String, String> sourceEnvVars = ImmutableMap.of(
        ClusterStatefulSetEnvVars.POSTGRES_VERSION.name(), sourceVersion,
        ClusterStatefulSetEnvVars.POSTGRES_MAJOR_VERSION.name(), sourceMajorVersion);
    String locale = majorVersionUpgradeStatus.getLocale();
    String encoding = majorVersionUpgradeStatus.getEncoding();
    String dataChecksum = majorVersionUpgradeStatus.getDataChecksum().toString();
    String link = majorVersionUpgradeStatus.getLink().toString();
    String clone = majorVersionUpgradeStatus.getClone().toString();
    String check = majorVersionUpgradeStatus.getCheck().toString();
    final String targetPatroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            targetVersion));
    return Stream.of(
        new ContainerBuilder()
            .withName(StackgresClusterContainers.MAJOR_VERSION_UPGRADE)
            .withImage(targetPatroniImageName)
            .withImagePullPolicy("IfNotPresent")
            .withCommand("/bin/sh", "-ex",
                ClusterStatefulSetPath.TEMPLATES_PATH.path()
                    + "/"
                    + ClusterStatefulSetPath.LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH.filename())
            .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
            .addToEnv(
                new EnvVarBuilder()
                    .withName("PRIMARY_INSTANCE")
                    .withValue(primaryInstance)
                    .build(),
                new EnvVarBuilder()
                    .withName("TARGET_VERSION")
                    .withValue(targetVersion)
                    .build(),
                new EnvVarBuilder()
                    .withName("SOURCE_VERSION")
                    .withValue(sourceVersion)
                    .build(),
                new EnvVarBuilder()
                    .withName("LOCALE")
                    .withValue(locale)
                    .build(),
                new EnvVarBuilder()
                    .withName("ENCODING")
                    .withValue(encoding)
                    .build(),
                new EnvVarBuilder()
                    .withName("DATA_CHECKSUM")
                    .withValue(dataChecksum)
                    .build(),
                new EnvVarBuilder()
                    .withName("LINK")
                    .withValue(link)
                    .build(),
                new EnvVarBuilder()
                    .withName("CLONE")
                    .withValue(clone)
                    .build(),
                new EnvVarBuilder()
                    .withName("CHECK")
                    .withValue(check)
                    .build(),
                new EnvVarBuilder()
                    .withName("POD_NAME")
                    .withValueFrom(new EnvVarSourceBuilder()
                        .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                        .build())
                    .build())
            .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(context),
                ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context))
            .addAllToVolumeMounts(ClusterStatefulSetVolumeConfig.USER.volumeMounts(context))
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
                        .withSubPath(
                            ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.subPath(context,
                                ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(context))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_BIN_PATH.subPath(context,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(
                            ClusterStatefulSetPath.PG_EXTENSIONS_MOUNTED_BIN_PATH.path(context))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(
                            ClusterStatefulSetPath.PG_EXTENSIONS_LIB64_PATH.subPath(context,
                                ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(
                            ClusterStatefulSetPath.PG_EXTENSIONS_MOUNTED_LIB64_PATH.path(context))))
            .addToVolumeMounts(
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.subPath(
                            context, sourceEnvVars,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(
                            ClusterStatefulSetPath.PG_LIB_PATH.path(context, sourceEnvVars))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_BIN_PATH.subPath(
                            context, sourceEnvVars,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(
                            ClusterStatefulSetPath.PG_BIN_PATH.path(context, sourceEnvVars))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_SHARE_PATH.subPath(
                            context, sourceEnvVars,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(
                            ClusterStatefulSetPath.PG_SHARE_PATH.path(context, sourceEnvVars))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.subPath(
                            context, sourceEnvVars,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(
                            context, sourceEnvVars))))
            .build(),
        new ContainerBuilder()
            .withName("reset-patroni-initialize")
            .withImage(StackGresComponent.KUBECTL.findLatestImageName())
            .withImagePullPolicy("IfNotPresent")
            .withCommand("/bin/sh", "-ex",
                ClusterStatefulSetPath.TEMPLATES_PATH.path()
                    + "/"
                    + ClusterStatefulSetPath.LOCAL_BIN_RESET_PATRONI_INITIALIZE_SH_PATH.filename())
            .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
            .addToEnv(
                new EnvVarBuilder()
                    .withName("PRIMARY_INSTANCE")
                    .withValue(primaryInstance)
                    .build(),
                new EnvVarBuilder()
                    .withName("POD_NAME")
                    .withValueFrom(new EnvVarSourceBuilder()
                        .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                        .build())
                    .build(),
                new EnvVarBuilder()
                    .withName("CLUSTER_NAMESPACE")
                    .withValue(context.getCluster().getMetadata().getNamespace())
                    .build(),
                new EnvVarBuilder()
                    .withName("PATRONI_ENDPOINT_NAME")
                    .withValue(patroniServices.configName(context))
                    .build())
            .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(context),
                ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
                ClusterStatefulSetVolumeConfig.USER.volumeMount(context),
                ClusterStatefulSetVolumeConfig.LOCAL_BIN.volumeMount(context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(
                            ClusterStatefulSetPath.LOCAL_BIN_RESET_PATRONI_INITIALIZE_SH_PATH
                                .filename())
                        .withMountPath(
                            ClusterStatefulSetPath.LOCAL_BIN_RESET_PATRONI_INITIALIZE_SH_PATH
                                .path())
                        .withReadOnly(true)))
            .build());
  }

}
