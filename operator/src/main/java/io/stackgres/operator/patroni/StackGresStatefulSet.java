/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HTTPGetActionBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.PodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.PodAntiAffinityBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ProbeBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyBuilder;
import io.stackgres.common.StackGresClusterConfig;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.customresource.sgprofile.StackGresProfile;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.configuration.ImmutableStorageConfig;
import io.stackgres.operator.configuration.StorageConfig;

public class StackGresStatefulSet {

  public static final String PATRONI_CONTAINER_NAME = "patroni";
  public static final String VOLUME_NAME = "pg-data";

  private static final String IMAGE_PREFIX = "docker.io/ongres/patroni:v%s-pg%s-build-%s";
  private static final String PATRONI_VERSION = "1.6.0";

  /**
   * Create a new StatefulSet based on the StackGresCluster definition.
   */
  public static List<HasMetadata> create(StackGresClusterConfig config) {
    final String name = config.getCluster().getMetadata().getName();
    final String namespace = config.getCluster().getMetadata().getNamespace();
    final String pgVersion = config.getCluster().getSpec().getPostgresVersion();
    final Optional<StackGresProfile> profile = config.getProfile();

    ResourceRequirements resources = new ResourceRequirements();
    StorageConfig storage = ImmutableStorageConfig.builder()
        .size(config.getCluster().getSpec().getVolumeSize())
        .storageClass(Optional.ofNullable(
            config.getCluster().getSpec().getStorageClass())
            .filter(storageClass -> storageClass.isEmpty())
            .orElse("standard"))
        .build();
    if (profile.isPresent()) {
      resources.setRequests(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
      resources.setLimits(ImmutableMap.of(
          "cpu", new Quantity(profile.get().getSpec().getCpu()),
          "memory", new Quantity(profile.get().getSpec().getMemory())));
    }

    PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(storage.getResourceRequirements())
        .withStorageClassName(storage.getStorageClass());

    Map<String, String> labels = ResourceUtil.defaultLabels(name);
    Map<String, String> podLabels = ImmutableMap.<String, String>builder()
        .putAll(labels)
        .put("disruptible", "true")
        .build();

    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")
        .build();

    VolumeMount pgData = new VolumeMountBuilder()
        .withName(VOLUME_NAME)
        .withMountPath("/var/lib/postgresql")
        .build();

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
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
            .withImagePullPolicy("Always")
            .withSecurityContext(new SecurityContextBuilder()
                .withRunAsUser(999L)
                .withAllowPrivilegeEscalation(Boolean.FALSE)
                .build())
            .withPorts(
                new ContainerPortBuilder().withContainerPort(5432).build(),
                new ContainerPortBuilder().withContainerPort(8008).build())
            .withVolumeMounts(pgSocket, pgData)
            .withEnvFrom(new EnvFromSourceBuilder()
                .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                    .withName(name).build())
                .build())
            .withEnv(
                new EnvVarBuilder().withName("PATRONI_NAME")
                    .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.name").build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_KUBERNETES_NAMESPACE")
                    .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                        new ObjectFieldSelectorBuilder().withFieldPath("metadata.namespace")
                            .build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_KUBERNETES_POD_IP")
                    .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                        new ObjectFieldSelectorBuilder().withFieldPath("status.podIP").build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_SUPERUSER_PASSWORD")
                    .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                        new SecretKeySelectorBuilder()
                            .withName(name)
                            .withKey("superuser-password")
                            .build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
                    .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                        new SecretKeySelectorBuilder()
                            .withName(name)
                            .withKey("replication-password")
                            .build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_authenticator_PASSWORD")
                    .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                        new SecretKeySelectorBuilder()
                            .withName(name)
                            .withKey("authenticator-password")
                            .build())
                        .build())
                    .build(),
                new EnvVarBuilder().withName("PATRONI_authenticator_OPTIONS")
                    .withValue("superuser")
                    .build())
            .withLivenessProbe(new ProbeBuilder()
                .withHttpGet(new HTTPGetActionBuilder()
                    .withPath("/health")
                    .withPort(new IntOrString(8008))
                    .withScheme("HTTP")
                    .build())
                .withInitialDelaySeconds(600)
                .withPeriodSeconds(60)
                .withFailureThreshold(5)
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
            .withResources(resources)
            .endContainer()
            .withVolumes(new VolumeBuilder()
                .withName("pg-socket")
                .withNewEmptyDir()
                .withMedium("Memory")
                .endEmptyDir()
                .build())
            .withTerminationGracePeriodSeconds(60L)
            .withInitContainers(new ContainerBuilder()
                .withName("data-permissions")
                .withImage("busybox")
                .withCommand("/bin/sh")
                .withArgs("-c", "chmod 755 /var/lib/postgresql "
                    + "&& chown 999:999 /var/lib/postgresql")
                .withVolumeMounts(pgData)
                .build())
            .addAllToContainers(config.getSidecars().stream()
                .map(sidecarEntry -> sidecarEntry.getSidecar().getContainer(config))
                .collect(ImmutableList.toImmutableList()))
            .addAllToVolumes(config.getSidecars().stream()
                .flatMap(sidecarEntry -> sidecarEntry.getSidecar().getVolumes(config).stream())
                .collect(ImmutableList.toImmutableList()))
            .endSpec()
            .build())
        .withVolumeClaimTemplates(new PersistentVolumeClaimBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(VOLUME_NAME)
                .withLabels(labels)
                .build())
            .withSpec(volumeClaimSpec.build())
            .build())
        .endSpec()
        .build();

    return ImmutableList.<HasMetadata>builder()
        .addAll(() -> config.getSidecars().stream()
            .flatMap(sidecarEntry -> sidecarEntry.getSidecar().getResources(config).stream())
            .iterator())
        .add(statefulSet)
        .build();
  }

}
