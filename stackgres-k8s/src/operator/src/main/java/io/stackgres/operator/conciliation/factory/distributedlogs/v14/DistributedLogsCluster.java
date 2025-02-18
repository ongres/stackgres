/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.v14;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;
import static io.stackgres.operator.common.StackGresDistributedLogsUtil.TIMESCALEDB_EXTENSION_NAME;
import static io.stackgres.operator.common.StackGresDistributedLogsUtil.getDefaultDistributedLogsExtensions;
import static io.stackgres.operator.common.StackGresDistributedLogsUtil.getPostgresVersion;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.CustomContainerBuilder;
import io.stackgres.common.crd.CustomVolumeBuilder;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.common.patroni.StackGresPasswordKeys;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsFlunetdConfigMap;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsPostgresConfig;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsScript;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsTemplatesConfigMap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder(stopAt = StackGresVersion.V_1_14)
public class DistributedLogsCluster
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  public static final String NAME = "fluentd";
  public static final int FORWARD_PORT = 12225;
  public static final String FORWARD_PORT_NAME = "forward";

  public static final String CONFIG = "fluentd-config";
  public static final String BUFFER = "fluentd-buffer";
  public static final String LOG = "fluentd-log";

  private final LabelFactoryForDistributedLogs labelFactory;

  @Inject
  public DistributedLogsCluster(
      LabelFactoryForDistributedLogs labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs distributedLogs = context.getSource();
    final StackGresCluster cluster = getCluster(
        labelFactory,
        distributedLogs,
        context.getCluster());
    return Stream.of(cluster);
  }

  private StackGresCluster getCluster(
      final LabelFactoryForDistributedLogs labelFactory,
      final StackGresDistributedLogs distributedLogs,
      final Optional<StackGresCluster> previousCluster) {
    final ObjectMeta metadata = distributedLogs.getMetadata();
    final String name = metadata.getName();
    final String namespace = metadata.getNamespace();

    final StackGresCluster cluster =
        new StackGresClusterBuilder(
            previousCluster
            .orElseGet(StackGresCluster::new))
        .editMetadata()
        .withAnnotations(
            previousCluster
            .map(StackGresCluster::getMetadata)
            .map(ObjectMeta::getAnnotations)
            .orElseGet(() -> Optional.of(distributedLogs)
                .map(StackGresDistributedLogs::getMetadata)
                .map(ObjectMeta::getAnnotations)
                .map(Map::entrySet)
                .stream()
                .flatMap(Set::stream)
                .filter(annotation -> annotation.getKey().equals(StackGresContext.VERSION_KEY))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
        .withLabels(labelFactory.genericLabels(distributedLogs))
        .withNamespace(namespace)
        .withName(name)
        .endMetadata()
        .editSpec()
        .withInstances(
            previousCluster
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getInstances)
            .orElse(1))
        .editPostgres()
        .withVersion(
            previousCluster
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getPostgres)
            .map(StackGresClusterPostgres::getVersion)
            .orElse(getPostgresVersion(distributedLogs)))
        .withExtensions(
            Seq.of(previousCluster
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getPostgres)
            .map(StackGresClusterPostgres::getExtensions)
            .orElse(List.of()))
            .flatMap(extensions -> Seq.seq(extensions)
                .append(getDefaultDistributedLogsExtensions(distributedLogs)
                    .stream()
                    .filter(extension -> extensions
                        .stream()
                        .map(StackGresClusterExtension::getName)
                        .noneMatch(extension.extensionName()::equals))
                    .map(extension -> new StackGresClusterExtensionBuilder()
                        .withName(extension.extensionName())
                        .withVersion(
                            Optional.of(distributedLogs)
                            .map(StackGresDistributedLogs::getStatus)
                            .map(StackGresDistributedLogsStatus::getTimescaledbVersion)
                            .filter(ignored -> extension.extensionName().equals(TIMESCALEDB_EXTENSION_NAME))
                            .or(() -> extension.extensionVersion())
                            .orElse(null))
                        .build())))
            .toList())
        .endPostgres()
        .editConfigurations()
        .withNewCredentials()
        .withNewUsers()
        .withNewSuperuser()
        .withNewPassword()
        .withName(DistributedLogsCredentials.secretName(distributedLogs))
        .withKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY)
        .endPassword()
        .endSuperuser()
        .withNewReplication()
        .withNewPassword()
        .withName(DistributedLogsCredentials.secretName(distributedLogs))
        .withKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY)
        .endPassword()
        .endReplication()
        .withNewAuthenticator()
        .withNewPassword()
        .withName(DistributedLogsCredentials.secretName(distributedLogs))
        .withKey(StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY)
        .endPassword()
        .endAuthenticator()
        .endUsers()
        .endCredentials()
        .withSgPostgresConfig(
            DistributedLogsPostgresConfig.configName(distributedLogs))
        .endConfigurations()
        .withPostgresServices(
            Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getPostgresServices)
            .orElse(new StackGresPostgresServices()))
        .editPostgresServices()
        .withPrimary(
            Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getPostgresServices)
            .map(StackGresPostgresServices::getPrimary)
            .orElse(new StackGresPostgresService()))
        .editPrimary()
        .withEnabled()
        .addNewCustomPort()
        .withName(FORWARD_PORT_NAME)
        .withTargetPort(new IntOrString(FORWARD_PORT_NAME))
        .withPort(FORWARD_PORT)
        .withProtocol("TCP")
        .endCustomPort()
        .endPrimary()
        .endPostgresServices()
        .withSgInstanceProfile(
            distributedLogs.getSpec().getSgInstanceProfile())
        .editPods()
        .withScheduling(Optional.of(distributedLogs)
            .map(StackGresDistributedLogs::getSpec)
            .map(StackGresDistributedLogsSpec::getScheduling)
            .orElse(null))
        .withCustomContainers(
            new CustomContainerBuilder()
            .withName(StackGresContainer.FLUENTD.getName())
            .withImage(StackGresComponent.FLUENTD.get(distributedLogs)
                .getLatestImageName())
            .withImagePullPolicy(getDefaultPullPolicy())
            .withCommand("/bin/sh", "-ex",
                ClusterPath.TEMPLATES_PATH.path()
                    + "/" + ClusterPath.LOCAL_BIN_START_FLUENTD_SH_PATH.filename())
            .withPorts(
                new ContainerPortBuilder()
                    .withProtocol("TCP")
                    .withName(FORWARD_PORT_NAME)
                    .withContainerPort(FORWARD_PORT)
                    .build())
            .withEnv(
                new EnvVarBuilder()
                .withName(ClusterPath.TEMPLATES_PATH.name())
                .withValue(ClusterPath.TEMPLATES_PATH.path())
                .build(),
                new EnvVarBuilder()
                .withName(ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH.name())
                .withValue(ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH.path())
                .build(),
                new EnvVarBuilder()
                .withName("FLUENTD_LAST_CONFIG_PATH")
                .withValue("/tmp/fluentd/last-fluentd-config")
                .build())
            .withVolumeMounts(
                new VolumeMountBuilder()
                .withName(StackGresVolume.USER.getName())
                .withMountPath(ClusterPath.ETC_PASSWD_PATH.path())
                .withSubPath(ClusterPath.ETC_PASSWD_PATH.subPath())
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(StackGresVolume.USER.getName())
                .withMountPath(ClusterPath.ETC_GROUP_PATH.path())
                .withSubPath(ClusterPath.ETC_GROUP_PATH.subPath())
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(StackGresVolume.USER.getName())
                .withMountPath(ClusterPath.ETC_SHADOW_PATH.path())
                .withSubPath(ClusterPath.ETC_SHADOW_PATH.subPath())
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(StackGresVolume.USER.getName())
                .withMountPath(ClusterPath.ETC_GSHADOW_PATH.path())
                .withSubPath(ClusterPath.ETC_GSHADOW_PATH.subPath())
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName(StackGresVolume.POSTGRES_SOCKET.getName())
                .withMountPath(ClusterPath.PG_RUN_PATH.path())
                .build(),
                new VolumeMountBuilder()
                .withName(StackGresVolume.CUSTOM.getName("templates"))
                .withMountPath(ClusterPath.TEMPLATES_PATH.path())
                .withReadOnly(Boolean.FALSE)
                .build(),
                new VolumeMountBuilder()
                .withName(StackGresVolume.CUSTOM.getName(
                    StackGresVolume.FLUENTD_CONFIG.getName()))
                .withMountPath("/etc/fluentd")
                .withReadOnly(Boolean.FALSE)
                .build(),
                new VolumeMountBuilder()
                .withName(StackGresVolume.CUSTOM.getName(
                    StackGresVolume.FLUENTD.getName()))
                .withMountPath("/tmp/fluentd")
                .withReadOnly(Boolean.FALSE)
                .build(),
                new VolumeMountBuilder()
                .withName(StackGresVolume.CUSTOM.getName(
                    StackGresVolume.FLUENTD_BUFFER.getName()))
                .withMountPath("/var/log/fluentd")
                .build())
            .build())
        .withCustomVolumes(
            new CustomVolumeBuilder()
            .withName("templates")
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(DistributedLogsTemplatesConfigMap.templatesName(distributedLogs))
                .withDefaultMode(0550)
                .build())
            .build(),
            new CustomVolumeBuilder()
            .withName(StackGresVolume.FLUENTD_CONFIG.getName())
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(DistributedLogsFlunetdConfigMap.configName(distributedLogs))
                .withDefaultMode(0440)
                .build())
            .build(),
            new CustomVolumeBuilder()
            .withName(StackGresVolume.FLUENTD.getName())
            .withNewEmptyDir()
            .endEmptyDir()
            .build(),
            new CustomVolumeBuilder()
            .withName(StackGresVolume.FLUENTD_BUFFER.getName())
            .withNewEmptyDir()
            .endEmptyDir()
            .build())
        .withPersistentVolume(
            Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getPersistentVolume)
            .orElseThrow())
        .withResources(
            Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getResources)
            .orElse(null))
        .endPods()
        .editManagedSql()
        .withScripts(Seq.of(previousCluster
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getManagedSql)
            .map(StackGresClusterManagedSql::getScripts)
            .orElse(List.of()))
            .flatMap(scripts -> Seq.seq(scripts)
                .append(Optional.of(DistributedLogsScript.scriptName(distributedLogs))
                    .filter(script -> scripts.stream()
                        .map(StackGresClusterManagedScriptEntry::getSgScript)
                        .noneMatch(script::equals))
                    .map(script -> new StackGresClusterManagedScriptEntryBuilder()
                        .withSgScript(script)
                        .build())))
            .toList())
        .endManagedSql()
        .withMetadata(
            Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getMetadata)
            .orElse(new StackGresClusterSpecMetadata()))
        .editMetadata()
        .withAnnotations(
            Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getMetadata)
            .map(StackGresClusterSpecMetadata::getAnnotations)
            .orElse(new StackGresClusterSpecAnnotations()))
        .editAnnotations()
        .withClusterPods(
            Seq.seq(Optional.of(distributedLogs.getSpec())
                .map(StackGresDistributedLogsSpec::getMetadata)
                .map(StackGresClusterSpecMetadata::getAnnotations)
                .map(StackGresClusterSpecAnnotations::getClusterPods)
                .orElse(Map.of()))
            .filter(entry -> !StackGresContext.FLUENTD_VERSION_KEY.equals(entry.v1))
            .append(Seq.seq(
                Map.of(
                    StackGresContext.FLUENTD_VERSION_KEY,
                    StackGresComponent.FLUENTD.get(distributedLogs).getLatestVersion())))
            .toMap(Tuple2::v1, Tuple2::v2))
        .endAnnotations()
        .endMetadata()
        .withProfile(Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getProfile)
            .orElse(null))
        .withNonProductionOptions(
            Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getNonProductionOptions)
            .orElse(null))
        .endSpec()
        .build();
    return cluster;
  }

  public static void main(String[] args) {
    System.out.println(StackGresVolume.CUSTOM.getName(
        StackGresVolume.FLUENTD_CONFIG.getName()));
  }
}
