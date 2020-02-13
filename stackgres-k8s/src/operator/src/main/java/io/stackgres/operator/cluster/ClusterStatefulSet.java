/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpecBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodAffinityTermBuilder;
import io.fabric8.kubernetes.api.model.PodAntiAffinityBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetUpdateStrategyBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.configuration.ImmutableStorageConfig;
import io.stackgres.operator.configuration.StorageConfig;
import io.stackgres.operator.patroni.Patroni;
import io.stackgres.operator.patroni.PatroniRole;
import io.stackgres.operatorframework.resource.ResourceUtil;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterStatefulSet implements StackGresClusterResourceStreamFactory {

  public static final String DATA_SUFFIX = "-data";
  public static final String BACKUP_SUFFIX = "-backup";

  public static final String SOCKET_VOLUME_NAME = "socket";
  public static final String GCS_CREDENTIALS_VOLUME_NAME = "gcs-credentials";
  public static final String GCS_RESTORE_CREDENTIALS_VOLUME_NAME = "gcs-restore-credentials";
  public static final String PATRONI_CONFIG_VOLUME_NAME = "patroni-config";
  public static final String BACKUP_CONFIG_VOLUME_NAME = "backup-config";
  public static final String BACKUP_SECRET_VOLUME_NAME = "backup-secret";
  public static final String RESTORE_CONFIG_VOLUME_NAME = "restore-config";
  public static final String RESTORE_SECRET_VOLUME_NAME = "restore-secret";
  public static final String RESTORE_ENTRYPOINT_VOLUME_NAME = "restore-entrypoint";
  public static final String LOCAL_BIN_VOLUME_NAME = "local-bin";

  public static final String PATRONI_ENV = "patroni";
  public static final String BACKUP_ENV = "backup";
  public static final String RESTORE_ENV = "restore";

  public enum ClusterStatefulSetPaths {
    LOCAL_BIN_PATH("/usr/local/bin"),
    PG_BASE_PATH("/var/lib/postgresql"),
    PG_RUN_PATH("/var/run/postgresql"),
    PG_DATA_PATH(PG_BASE_PATH, "data"),
    BASE_ENV_PATH("/etc/env"),
    BASE_SECRET_PATH(BASE_ENV_PATH, ".secret"),
    PATRONI_ENV_PATH(BASE_ENV_PATH, PATRONI_ENV),
    BACKUP_PATH(PG_BASE_PATH, "backups"),
    BACKUP_ENV_PATH(BASE_ENV_PATH, BACKUP_ENV),
    BACKUP_SECRET_PATH(BASE_SECRET_PATH, BACKUP_ENV),
    RESTORE_ENTRYPOINT_PATH("/etc/patroni/restore"),
    RESTORE_ENV_PATH(BASE_ENV_PATH, RESTORE_ENV),
    RESTORE_SECRET_PATH(BASE_SECRET_PATH, RESTORE_ENV);

    private final String path;

    private ClusterStatefulSetPaths(String path) {
      this.path = path;
    }

    private ClusterStatefulSetPaths(ClusterStatefulSetPaths parent, String...paths) {
      this.path = parent.path + Arrays.asList(paths).stream().collect(Collectors.joining("/"));
    }

    public String path() {
      return path;
    }
  }

  public static final String GCS_CREDENTIALS_FILE_NAME = "gcs-credentials.json";

  private final Patroni patroni;
  private final ClusterStatefulSetInitContainers initContainerFactory;
  private final ClusterStatefulSetVolumes volumesFactory;

  @Inject
  public ClusterStatefulSet(Patroni patroni, ClusterStatefulSetInitContainers initContainerFactory,
      ClusterStatefulSetVolumes volumesFactory) {
    super();
    this.patroni = patroni;
    this.initContainerFactory = initContainerFactory;
    this.volumesFactory = volumesFactory;
  }

  public static String dataName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + ClusterStatefulSet.DATA_SUFFIX);
  }

  public static String backupName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + ClusterStatefulSet.BACKUP_SUFFIX);
  }

  /**
   * Create a new StatefulSet based on the StackGresCluster definition.
   */
  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    StackGresClusterContext clusterContext = context.getClusterContext();

    final String name = clusterContext.getCluster().getMetadata().getName();
    final String namespace = clusterContext.getCluster().getMetadata().getNamespace();

    StorageConfig dataStorageConfig = ImmutableStorageConfig.builder()
        .size(clusterContext.getCluster().getSpec().getVolumeSize())
        .storageClass(Optional.ofNullable(
            clusterContext.getCluster().getSpec().getStorageClass())
            .orElse(null))
        .build();

    final PersistentVolumeClaimSpecBuilder volumeClaimSpec = new PersistentVolumeClaimSpecBuilder()
        .withAccessModes("ReadWriteOnce")
        .withResources(dataStorageConfig.getResourceRequirements())
        .withStorageClassName(dataStorageConfig.getStorageClass());

    final Map<String, String> labels = StackGresUtil.clusterLabels(clusterContext.getCluster());
    final Map<String, String> podLabels = StackGresUtil.statefulSetPodLabels(
        clusterContext.getCluster());

    StatefulSet clusterStatefulSet = new StatefulSetBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(
            clusterContext.getCluster())))
        .endMetadata()
        .withNewSpec()
        .withReplicas(clusterContext.getCluster().getSpec().getInstances())
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
            .withAffinity(Optional.of(new AffinityBuilder()
                .withPodAntiAffinity(new PodAntiAffinityBuilder()
                    .addAllToRequiredDuringSchedulingIgnoredDuringExecution(ImmutableList.of(
                        new PodAffinityTermBuilder()
                            .withLabelSelector(new LabelSelectorBuilder()
                                .withMatchExpressions(new LabelSelectorRequirementBuilder()
                                        .withKey(StackGresUtil.APP_KEY)
                                        .withOperator("In")
                                        .withValues(StackGresUtil.APP_NAME)
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
                .filter(affinity -> Optional.ofNullable(
                    clusterContext.getCluster().getSpec().getNonProduction())
                    .map(nonProduction -> nonProduction.getDisableClusterPodAntiAffinity())
                    .map(disableClusterPodAntiAffinity -> !disableClusterPodAntiAffinity)
                    .orElse(true))
                .orElse(null))
            .withShareProcessNamespace(Boolean.TRUE)
            .withServiceAccountName(PatroniRole.roleName(clusterContext))
            .withVolumes(volumesFactory.listResources(clusterContext))
            .withTerminationGracePeriodSeconds(60L)
            .addToContainers(patroni.getContainer(context))
            .addAllToVolumes(patroni.getVolumes(context))
            .withInitContainers(initContainerFactory.listResources(clusterContext))
            .addAllToContainers(clusterContext.getSidecars().stream()
                .map(sidecarEntry -> sidecarEntry.getSidecar().getContainer(context))
                .collect(ImmutableList.toImmutableList()))
            .addAllToVolumes(clusterContext.getSidecars().stream()
                .flatMap(sidecarEntry -> sidecarEntry.getSidecar().getVolumes(context).stream())
                .collect(ImmutableList.toImmutableList()))
            .endSpec()
            .build())
        .withVolumeClaimTemplates(Stream.of(
            Stream.of(new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(dataName(clusterContext))
                .withLabels(labels)
                .endMetadata()
                .withSpec(volumeClaimSpec.build())
                .build()))
            .flatMap(s -> s)
            .toArray(PersistentVolumeClaim[]::new))
        .endSpec()
        .build();

    return Seq.<HasMetadata>empty()
        .append(patroni.streamResources(context))
        .append(clusterContext.getSidecars().stream()
            .flatMap(sidecarEntry -> sidecarEntry.getSidecar().streamResources(context)))
        .append(Seq.seq(context.getExistingResources())
            .filter(existingResource -> existingResource instanceof Pod)
            .map(HasMetadata::getMetadata)
            .filter(existingPodMetadata -> Objects.equals(
                existingPodMetadata.getLabels().get(StackGresUtil.CLUSTER_KEY),
                Boolean.TRUE.toString()))
            .map(existingPodMetadata -> new PodBuilder()
                .withNewMetadata()
                .withNamespace(existingPodMetadata.getNamespace())
                .withName(existingPodMetadata.getName())
                .withLabels(podLabels)
                .endMetadata()
                .build()))
        .append(clusterStatefulSet);
  }

}
