/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.TCPSocketActionBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.CustomContainerBuilder;
import io.stackgres.common.crd.CustomVolumeBuilder;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsConfigurations;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder
public class DistributedLogsCluster
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  private final LabelFactoryForDistributedLogs labelFactory;

  @Inject
  public DistributedLogsCluster(
      LabelFactoryForDistributedLogs labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs distributedLogs = context.getSource();
    final StackGresCluster cluster = getCluster(labelFactory, distributedLogs);
    return Stream.of(cluster);
  }

  public static StackGresCluster getCluster(
      final LabelFactoryForDistributedLogs labelFactory,
      final StackGresDistributedLogs distributedLogs) {
    final ObjectMeta metadata = distributedLogs.getMetadata();
    final String name = metadata.getName();
    final String namespace = metadata.getNamespace();

    final StackGresCluster cluster = new StackGresClusterBuilder()
        .withNewMetadata()
        .withLabels(labelFactory.genericLabels(distributedLogs))
        .withNamespace(namespace)
        .withName(name)
        .endMetadata()
        .withNewSpec()
        .withInstances(1)
        .withNewPostgres()
        .withVersion(StackGresDistributedLogsUtil.getPostgresVersion(distributedLogs))
        .withExtensions(
            StackGresUtil.getDefaultDistributedLogsExtensions(distributedLogs)
            .stream()
            .map(extension -> new StackGresClusterExtensionBuilder()
                .withName(extension.extensionName())
                .withVersion(
                    extension.extensionVersion()
                    .orElse(null))
                .build())
            .toList())
        .endPostgres()
        .withNewConfigurations()
        .withSgPostgresConfig(
            Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getConfigurations)
            .map(StackGresDistributedLogsConfigurations::getSgPostgresConfig)
            .orElseThrow())
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
        .endPrimary()
        .endPostgresServices()
        .withNewPods()
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
            .withCommand("/bin/sh", "-exc")
            .withArgs(
                """
                echo 'Wait for postgres to be up, running and initialized'
                until curl -s localhost:8008/readiness --fail > /dev/null; do sleep 1; done
                mkdir -p /tmp/fluentd
                chmod 700 /tmp/fluentd
                export TMPDIR=/tmp/fluentd
                exec /usr/local/bin/fluentd -c /etc/fluentd/fluentd.conf
                """)
            .withPorts(
                new ContainerPortBuilder()
                    .withProtocol("TCP")
                    .withName(FluentdUtil.FORWARD_PORT_NAME)
                    .withContainerPort(FluentdUtil.FORWARD_PORT)
                    .build())
            .withLivenessProbe(new ProbeBuilder()
                .withTcpSocket(new TCPSocketActionBuilder()
                    .withPort(new IntOrString(String.valueOf(FluentdUtil.FORWARD_PORT)))
                    .build())
                .withInitialDelaySeconds(15)
                .withPeriodSeconds(20)
                .withFailureThreshold(6)
                .build())
            .withReadinessProbe(new ProbeBuilder()
                .withTcpSocket(new TCPSocketActionBuilder()
                    .withPort(new IntOrString(String.valueOf(FluentdUtil.FORWARD_PORT)))
                    .build())
                .withInitialDelaySeconds(5)
                .withPeriodSeconds(10)
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
                .withName(StackGresVolume.CUSTOM.getName(
                    StackGresVolume.FLUENTD_CONFIG.getName()))
                .withMountPath("/etc/fluentd")
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
            .withName(StackGresVolume.FLUENTD_CONFIG.getName())
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(FluentdUtil.configName(distributedLogs))
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
            .build(),
            new CustomVolumeBuilder()
            .withName(StackGresVolume.FLUENTD_LOG.getName())
            .withNewEmptyDir()
            .endEmptyDir()
            .build())
        .withPersistentVolume(
            Optional.of(distributedLogs.getSpec())
            .map(StackGresDistributedLogsSpec::getPersistentVolume)
            .orElseThrow())
        .endPods()
        .withNewManagedSql()
        .addNewScript()
        .withSgScript(DistributedLogsScript.scriptName(distributedLogs))
        .endScript()
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

}
