/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.KeyToPathBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.PodAntiAffinityBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.TCPSocketActionBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyBuilder;
import io.fabric8.kubernetes.api.model.batch.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.JobTemplateSpecBuilder;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.configuration.ImmutableStorageConfig;
import io.stackgres.operator.configuration.StorageConfig;
import io.stackgres.operator.controller.ResourceGeneratorContext;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operator.sidecars.envoy.Envoy;

import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

public class StackGresStatefulSet {

  public static final String PATRONI_CONTAINER_NAME = "patroni";
  public static final String DATA_SUFFIX = "-data";
  public static final String BACKUP_SUFFIX = "-backup";
  public static final String SOCKET_VOLUME_NAME = "socket";
  public static final String PG_VOLUME_PATH = "/var/lib/postgresql";
  public static final String DATA_VOLUME_PATH = PG_VOLUME_PATH + "/data";
  public static final String BACKUP_VOLUME_PATH = PG_VOLUME_PATH + "/backups";
  public static final String GCS_CREDENTIALS_VOLUME_NAME = "gcs-credentials";
  public static final String WAL_G_WRAPPER_VOLUME_NAME = "wal-g-wrapper";

  private static final String IMAGE_PREFIX = "docker.io/ongres/patroni:v%s-pg%s-build-%s";
  private static final String PATRONI_VERSION = "1.6.1";
  private static final String GCS_CONFIG_PATH = "/.gcs";
  private static final String GCS_CREDENTIALS_FILE_NAME = "google-service-account-key.json";

  /**
   * Create a new StatefulSet based on the StackGresCluster definition.
   */
  public static List<HasMetadata> create(ResourceGeneratorContext context) {
    StackGresClusterConfig config = context.getClusterConfig();
    final String name = config.getCluster().getMetadata().getName();
    final String namespace = config.getCluster().getMetadata().getNamespace();
    final String pgVersion = config.getCluster().getSpec().getPostgresVersion();
    final Optional<StackGresProfile> profile = config.getProfile();

    ResourceRequirements podResources = new ResourceRequirements();
    if (profile.isPresent()) {
      podResources.setRequests(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
      podResources.setLimits(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
    }

    StorageConfig dataStorageConfig = ImmutableStorageConfig.builder()
        .size(config.getCluster().getSpec().getVolumeSize())
        .storageClass(Optional.ofNullable(
            config.getCluster().getSpec().getStorageClass())
            .orElse(null))
        .build();
    final PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(dataStorageConfig.getResourceRequirements())
        .withStorageClassName(dataStorageConfig.getStorageClass());

    final Map<String, String> labels = ResourceUtil.defaultLabels(name);
    final Map<String, String> podLabels = ResourceUtil.statefulSetPodLabels(name);

    ImmutableList.Builder<EnvVar> environmentsBuilder = ImmutableList.<EnvVar>builder().add(
        new EnvVarBuilder().withName("PATRONI_NAME")
        .withValueFrom(new EnvVarSourceBuilder()
            .withFieldRef(
                new ObjectFieldSelectorBuilder()
                .withFieldPath("metadata.name").build())
            .build())
        .build(),
        new EnvVarBuilder().withName("PATRONI_KUBERNETES_NAMESPACE")
        .withValueFrom(new EnvVarSourceBuilder()
            .withFieldRef(
                new ObjectFieldSelectorBuilder()
                .withFieldPath("metadata.namespace")
                .build())
            .build())
        .build(),
        new EnvVarBuilder().withName("PATRONI_KUBERNETES_POD_IP")
        .withValueFrom(
            new EnvVarSourceBuilder()
            .withFieldRef(
                new ObjectFieldSelectorBuilder()
                .withFieldPath("status.podIP")
                .build())
            .build())
        .build(),
        new EnvVarBuilder().withName("PATRONI_SUPERUSER_PASSWORD")
        .withValueFrom(new EnvVarSourceBuilder()
            .withSecretKeyRef(
                new SecretKeySelectorBuilder()
                .withName(name)
                .withKey("superuser-password")
                .build())
            .build())
        .build(),
        new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
        .withValueFrom(new EnvVarSourceBuilder()
            .withSecretKeyRef(
                new SecretKeySelectorBuilder()
                .withName(name)
                .withKey("replication-password")
                .build())
            .build())
        .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_PASSWORD")
        .withValueFrom(new EnvVarSourceBuilder()
            .withSecretKeyRef(
                new SecretKeySelectorBuilder()
                .withName(name)
                .withKey("authenticator-password")
                .build())
            .build())
        .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_OPTIONS")
        .withValue("superuser")
        .build());

    if (config.getBackupConfig()
        .map(backupConfig -> backupConfig.getSpec().getPgpConfiguration())
        .isPresent()) {
      environmentsBuilder.add(
          new EnvVarBuilder()
          .withName("WALG_PGP_KEY")
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(
                  config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getPgpConfiguration())
                    .map(pgpConfiguration -> pgpConfiguration.getKey()).get())
              .build())
          .build());
    }

    final Optional<PersistentVolumeClaim> backupVolumeClaim = config.getBackupConfig()
        .map(backupConfig -> backupConfig.getSpec().getStorage().getVolume())
        .map(volume -> {
          StorageConfig backupStorageConfig = ImmutableStorageConfig.builder()
              .size(volume.getSize())
              .build();
          return new PersistentVolumeClaimBuilder()
              .withNewMetadata()
              .withName(name + BACKUP_SUFFIX)
              .withNamespace(namespace)
              .withLabels(labels)
              .withOwnerReferences(ImmutableList.of(
                  ResourceUtil.getOwnerReference(config.getCluster())))
              .endMetadata()
              .withNewSpec()
              .withAccessModes("ReadWriteMany")
              .withStorageClassName(volume.getWriteManyStorageClass())
              .withResources(backupStorageConfig.getResourceRequirements())
              .endSpec()
              .build();
        });

    if (config.getBackupConfig()
        .map(backupConfig -> backupConfig.getSpec().getStorage().getS3())
        .isPresent()) {
      environmentsBuilder.add(
          new EnvVarBuilder()
          .withName("AWS_ACCESS_KEY_ID")
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(
                  config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getStorage().getS3())
                    .map(s3Storage -> s3Storage.getCredentials())
                    .map(awsCredentials -> awsCredentials.getAccessKey()).get())
              .build())
          .build(),
          new EnvVarBuilder()
          .withName("AWS_SECRET_ACCESS_KEY")
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(
                  config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getStorage().getS3())
                    .map(s3Storage -> s3Storage.getCredentials())
                    .map(awsCredentials -> awsCredentials.getSecretKey()).get())
              .build())
          .build());
    }

    if (config.getBackupConfig()
        .map(backupConfig -> backupConfig.getSpec().getStorage().getGcs())
        .isPresent()) {
      environmentsBuilder.add(
          new EnvVarBuilder()
          .withName("GOOGLE_APPLICATION_CREDENTIALS")
          .withValue(GCS_CONFIG_PATH + "/" + GCS_CREDENTIALS_FILE_NAME)
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(
                  config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getStorage().getGcs())
                    .map(s3Storage -> s3Storage.getCredentials())
                    .map(awsCredentials -> awsCredentials.getServiceAccountJsonKey()).get())
              .build())
          .build());
    }

    if (config.getBackupConfig()
        .map(backupConfig -> backupConfig.getSpec().getStorage().getAzureblob())
        .isPresent()) {
      environmentsBuilder.add(
          new EnvVarBuilder()
          .withName("AZURE_STORAGE_ACCOUNT")
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(
                  config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getStorage().getAzureblob())
                    .map(s3Storage -> s3Storage.getCredentials())
                    .map(awsCredentials -> awsCredentials.getAccount()).get())
              .build())
          .build(),
          new EnvVarBuilder()
          .withName("AZURE_STORAGE_ACCESS_KEY")
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(
                  config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getStorage().getAzureblob())
                    .map(s3Storage -> s3Storage.getCredentials())
                    .map(awsCredentials -> awsCredentials.getAccessKey()).get())
              .build())
          .build());
    }

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(config.getCluster())))
        .endMetadata()
        .withNewSpec()
        .withReplicas(config.getCluster().getSpec().getInstances())
        .withSelector(new LabelSelectorBuilder()
            .addToMatchLabels(podLabels)
            .build())
        .withUpdateStrategy(new StatefulSetUpdateStrategyBuilder()
            .withType("OnDelete")
            .build())
        .withServiceName(name)
        .withTemplate(new PodTemplateSpecBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .addToLabels(podLabels)
                .build())
            .withNewSpec()
            .withAffinity(new AffinityBuilder()
                .withPodAntiAffinity(new PodAntiAffinityBuilder()
                    .addAllToRequiredDuringSchedulingIgnoredDuringExecution(ImmutableList.of(
                        new PodAffinityTermBuilder()
                        .withLabelSelector(new LabelSelectorBuilder()
                            .withMatchExpressions(new LabelSelectorRequirementBuilder()
                                .withKey(ResourceUtil.APP_KEY)
                                .withOperator("In")
                                .withValues(ResourceUtil.APP_NAME)
                                .build(),
                                new LabelSelectorRequirementBuilder()
                                .withKey("cluster")
                                .withOperator("In")
                                .withValues("true")
                                .build())
                            .build())
                        .withTopologyKey("kubernetes.io/hostname")
                        .build()))
                    .build())
                .build())
            .withShareProcessNamespace(Boolean.TRUE)
            .withServiceAccountName(name + PatroniRole.SUFFIX)
            .addNewContainer()
            .withName(PATRONI_CONTAINER_NAME)
            .withImage(String.format(IMAGE_PREFIX,
                PATRONI_VERSION, pgVersion, StackGresUtil.CONTAINER_BUILD))
            .withCommand("/bin/sh", "-exc", Unchecked.supplier(() -> Resources
                .asCharSource(Class.class.getResource("/start-patroni.sh"),
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
            .withVolumeMounts(Stream.of(
                Stream.of(
                new VolumeMountBuilder()
                .withName(SOCKET_VOLUME_NAME)
                .withMountPath("/run/postgresql")
                .build(),
                new VolumeMountBuilder()
                .withName(name + DATA_SUFFIX)
                .withMountPath(PG_VOLUME_PATH)
                .build(),
                new VolumeMountBuilder()
                .withName(WAL_G_WRAPPER_VOLUME_NAME)
                .withMountPath("/wal-g-wrapper")
                .build()),
                Stream.of(config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getStorage().getVolume()))
                .filter(Optional::isPresent)
                .map(volumeStorage -> new VolumeMountBuilder()
                    .withName(name + BACKUP_SUFFIX)
                    .withMountPath(BACKUP_VOLUME_PATH)
                    .build()),
                Stream.of(config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getStorage().getGcs()))
                .filter(Optional::isPresent)
                .map(gcsStorage -> new VolumeMountBuilder()
                    .withName(GCS_CREDENTIALS_VOLUME_NAME)
                    .withMountPath(GCS_CONFIG_PATH)
                    .build()))
                .flatMap(stream -> stream)
                .collect(ImmutableList.toImmutableList()))
            .withEnvFrom(new EnvFromSourceBuilder()
                .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                    .withName(name).build())
                .build())
            .withEnv(environmentsBuilder.build())
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
            .endContainer()
            .withVolumes(Stream.of(
                Stream.of(
                    new VolumeBuilder()
                    .withName(SOCKET_VOLUME_NAME)
                    .withNewEmptyDir()
                    .withMedium("Memory")
                    .endEmptyDir()
                    .build(),
                    new VolumeBuilder()
                    .withName(WAL_G_WRAPPER_VOLUME_NAME)
                    .withNewEmptyDir()
                    .withMedium("Memory")
                    .endEmptyDir()
                    .build()),
                Stream.of(config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getStorage().getVolume()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(volumeStorage -> new VolumeBuilder()
                    .withName(name + BACKUP_SUFFIX)
                    .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSource(
                        name + BACKUP_SUFFIX, false))
                    .build()),
                Stream.of(config.getBackupConfig()
                    .map(backupConfig -> backupConfig.getSpec().getStorage().getGcs()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(gcsStorage -> new VolumeBuilder()
                    .withName(GCS_CREDENTIALS_VOLUME_NAME)
                    .withSecret(new SecretVolumeSourceBuilder()
                        .withSecretName(gcsStorage.getCredentials()
                            .getServiceAccountJsonKey().getName())
                        .withItems(new KeyToPathBuilder()
                            .withKey(gcsStorage.getCredentials()
                                .getServiceAccountJsonKey().getKey())
                            .withPath(GCS_CREDENTIALS_FILE_NAME)
                            .build())
                        .build())
                    .build()))
                .flatMap(stream -> stream)
                .collect(ImmutableList.toImmutableList()))
            .withTerminationGracePeriodSeconds(60L)
            .withInitContainers(
                new ContainerBuilder()
                .withName("data-permissions")
                .withImage("busybox")
                .withCommand("/bin/sh", "-ecx", Stream.of(
                    Stream.of(config.getBackupConfig()
                        .map(backupConfig -> backupConfig.getSpec().getStorage().getVolume()))
                    .filter(Optional::isPresent)
                    .map(volumeStorage -> "mkdir -p " + BACKUP_VOLUME_PATH
                        + "/" + namespace + "/" + name),
                    Stream.of(
                    "chmod -R 755 " + PG_VOLUME_PATH,
                    "chown -R 999:999 " + PG_VOLUME_PATH))
                    .flatMap(s -> s)
                    .collect(Collectors.joining(" && ")))
                .withVolumeMounts(Stream.of(
                    Stream.of(new VolumeMountBuilder()
                        .withName(name + DATA_SUFFIX)
                        .withMountPath(PG_VOLUME_PATH)
                        .build()),
                    Stream.of(config.getBackupConfig()
                        .map(backupConfig -> backupConfig.getSpec().getStorage().getVolume()))
                    .filter(Optional::isPresent)
                    .map(volumeStorage -> new VolumeMountBuilder()
                        .withName(name + BACKUP_SUFFIX)
                        .withMountPath(BACKUP_VOLUME_PATH)
                        .build()))
                    .flatMap(s -> s)
                    .toArray(VolumeMount[]::new))
                .build(),
                new ContainerBuilder()
                .withName("wal-g-wrapper")
                .withImage("busybox")
                .withCommand("/bin/sh", "-ecx", Unchecked.supplier(() -> Resources
                    .asCharSource(Class.class.getResource("/create-wal-g-wrapper.sh"),
                        StandardCharsets.UTF_8)
                    .read()).get())
                .withEnvFrom(new EnvFromSourceBuilder()
                    .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                        .withName(name).build())
                    .build())
                .withEnv(environmentsBuilder.build())
                .withVolumeMounts(
                    new VolumeMountBuilder()
                    .withName(WAL_G_WRAPPER_VOLUME_NAME)
                    .withMountPath("/wal-g-wrapper")
                    .build())
                .build())
            .addAllToContainers(config.getSidecars().stream()
                .map(sidecarEntry -> sidecarEntry.getSidecar().getContainer(context))
                .collect(ImmutableList.toImmutableList()))
            .addAllToVolumes(config.getSidecars().stream()
                .flatMap(sidecarEntry -> sidecarEntry.getSidecar().getVolumes(context).stream())
                .collect(ImmutableList.toImmutableList()))
            .endSpec()
            .build())
        .withVolumeClaimTemplates(Stream.of(
            Stream.of(new PersistentVolumeClaimBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(name + DATA_SUFFIX)
            .withLabels(labels)
            .endMetadata()
            .withSpec(volumeClaimSpec.build())
            .build()))
            .flatMap(s -> s)
            .toArray(PersistentVolumeClaim[]::new))
        .endSpec()
        .build();

    return ImmutableList.<HasMetadata>builder()
        .addAll(config.getSidecars().stream()
            .flatMap(sidecarEntry -> sidecarEntry.getSidecar().getResources(context).stream())
            .iterator())
        .addAll(Stream.of(backupVolumeClaim)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .iterator())
        .addAll(Stream.of(config.getBackupConfig())
            .filter(Optional::isPresent)
            .map(Unchecked.function(backupConfig -> new CronJobBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(name + BACKUP_SUFFIX)
                .withLabels(labels)
                .withOwnerReferences(ImmutableList.of(
                    ResourceUtil.getOwnerReference(config.getCluster())))
                .endMetadata()
                .withNewSpec()
                .withConcurrencyPolicy("Replace")
                .withFailedJobsHistoryLimit(10)
                .withStartingDeadlineSeconds(config.getBackupConfig()
                    .map(StackGresBackupConfig::getSpec)
                    .map(StackGresBackupConfigSpec::getFullWindow)
                    .orElse(5) * 60L)
                .withSchedule(config.getBackupConfig()
                    .map(StackGresBackupConfig::getSpec)
                    .map(StackGresBackupConfigSpec::getFullSchedule)
                    .orElse("0 5 * * *"))
                .withJobTemplate(new JobTemplateSpecBuilder()
                    .withNewMetadata()
                    .withNamespace(namespace)
                    .withName(name + BACKUP_SUFFIX)
                    .withLabels(labels)
                    .endMetadata()
                    .withNewSpec()
                    .withNewTemplate()
                    .withNewMetadata()
                    .withNamespace(namespace)
                    .withName(name + BACKUP_SUFFIX)
                    .withLabels(ImmutableMap.<String, String>builder()
                        .putAll(labels)
                        .put("role", "backup")
                        .build())
                    .endMetadata()
                    .withNewSpec()
                    .withRestartPolicy("OnFailure")
                    .withServiceAccountName(name + PatroniRole.SUFFIX)
                    .withContainers(new ContainerBuilder()
                        .withName(name + BACKUP_SUFFIX)
                        .withImage("bitnami/kubectl:latest")
                        .withEnv(
                            new EnvVarBuilder()
                            .withName("CLUSTER_NAMESPACE")
                            .withValue(namespace)
                            .build(),
                            new EnvVarBuilder()
                            .withName("CLUSTER_NAME")
                            .withValue(name)
                            .build(),
                            new EnvVarBuilder()
                            .withName("CLUSTER_LABELS")
                            .withValue(labels
                                .entrySet()
                                .stream()
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .collect(Collectors.joining(",")))
                            .build(),
                            new EnvVarBuilder().withName("POD_NAME")
                            .withValueFrom(
                                new EnvVarSourceBuilder()
                                .withFieldRef(
                                    new ObjectFieldSelectorBuilder()
                                    .withFieldPath("metadata.name")
                                    .build())
                                .build())
                            .build(),
                            new EnvVarBuilder()
                            .withName("RETAIN")
                            .withValue(config.getBackupConfig()
                                .map(StackGresBackupConfig::getSpec)
                                .map(StackGresBackupConfigSpec::getRetention)
                                .map(String::valueOf)
                                .orElse("5"))
                            .build(),
                            new EnvVarBuilder()
                            .withName("WINDOW")
                            .withValue(config.getBackupConfig()
                                .map(StackGresBackupConfig::getSpec)
                                .map(StackGresBackupConfigSpec::getFullWindow)
                                .map(window -> window * 60)
                                .map(String::valueOf)
                                .orElse("3600"))
                            .build())
                        .withCommand("/bin/bash", "-ecx", Resources
                            .asCharSource(Class.class.getResource("/backup-cronjob.sh"),
                                StandardCharsets.UTF_8)
                            .read())
                        .build())
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build())
                .endSpec()
                .build()))
            .iterator())
        .addAll(Seq.seq(context.getExistingResources())
            .filter(existingResource -> existingResource instanceof Pod)
            .map(existingPod -> existingPod.getMetadata())
            .filter(existingPodMetadata -> Objects.equals(
                existingPodMetadata.getLabels().get(ResourceUtil.CLUSTER_KEY),
                Boolean.TRUE.toString()))
            .map(existingPodMetadata -> new PodBuilder()
                .withNewMetadata()
                .withNamespace(existingPodMetadata.getNamespace())
                .withName(existingPodMetadata.getName())
                .withLabels(podLabels)
                .endMetadata()
                .build()))
        .add(statefulSet)
        .build();
  }

}
