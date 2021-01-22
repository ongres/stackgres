/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.apiweb.dto.cluster.ClusterConfiguration;
import io.stackgres.apiweb.dto.cluster.ClusterDistributedLogs;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterInitData;
import io.stackgres.apiweb.dto.cluster.ClusterNonProduction;
import io.stackgres.apiweb.dto.cluster.ClusterPod;
import io.stackgres.apiweb.dto.cluster.ClusterPodMetadata;
import io.stackgres.apiweb.dto.cluster.ClusterPodPersistentVolume;
import io.stackgres.apiweb.dto.cluster.ClusterPostgresService;
import io.stackgres.apiweb.dto.cluster.ClusterPostgresServices;
import io.stackgres.apiweb.dto.cluster.ClusterRestore;
import io.stackgres.apiweb.dto.cluster.ClusterScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterScriptFrom;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.apiweb.dto.cluster.ClusterSpecAnnotations;
import io.stackgres.apiweb.dto.cluster.ClusterSpecMetadata;
import io.stackgres.apiweb.dto.cluster.PodScheduling;
import io.stackgres.common.ArcUtil;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresPodScheduling;
import io.stackgres.common.crd.sgcluster.StackgresClusterConfiguration;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterTransformer
    extends AbstractResourceTransformer<ClusterDto, StackGresCluster> {

  private final StackGresPropertyContext<WebApiProperty> context;
  private final ClusterPodTransformer clusterPodTransformer;

  @Inject
  public ClusterTransformer(StackGresPropertyContext<WebApiProperty> context,
      ClusterPodTransformer clusterPodTransformer) {
    super();
    this.context = context;
    this.clusterPodTransformer = clusterPodTransformer;
  }

  public ClusterTransformer() {
    this.context = null;
    this.clusterPodTransformer = null;
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  public StackGresCluster toCustomResource(ClusterDto source, StackGresCluster original) {
    StackGresCluster transformation = Optional.ofNullable(original)
        .orElseGet(StackGresCluster::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public ClusterDto toDto(StackGresCluster source) {
    ClusterDto transformation = new ClusterDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setGrafanaEmbedded(isGrafanaEmbeddedEnabled());
    return transformation;
  }

  public ClusterDto toResourceWithPods(StackGresCluster source, List<Pod> pods) {
    ClusterDto clusterDto = toDto(source);

    clusterDto.setPods(Seq.seq(pods)
        .map(clusterPodTransformer::toResource)
        .toList());

    clusterDto.setPodsReady((int) clusterDto.getPods()
        .stream()
        .filter(pod -> pod.getContainers().equals(pod.getContainersReady()))
        .count());

    return clusterDto;
  }

  private boolean isGrafanaEmbeddedEnabled() {
    return context.getBoolean(WebApiProperty.GRAFANA_EMBEDDED);
  }

  public StackGresClusterSpec getCustomResourceSpec(ClusterSpec source) {
    if (source == null) {
      return null;
    }
    StackGresClusterSpec transformation = new StackGresClusterSpec();

    final ClusterConfiguration sourceClusterConfiguration = source.getConfigurations();

    if (sourceClusterConfiguration != null) {
      transformation.setConfiguration(new StackgresClusterConfiguration());
      transformation.getConfiguration().setBackupConfig(
          source.getConfigurations().getSgBackupConfig());
      transformation.getConfiguration()
          .setConnectionPoolingConfig(source.getConfigurations().getSgPoolingConfig());
      transformation.getConfiguration().setPostgresConfig(
          source.getConfigurations().getSgPostgresConfig());
    }
    transformation.setInstances(source.getInstances());
    transformation.setNonProduction(
        getCustomResourceNonProduction(source.getNonProduction()));
    transformation.setPostgresVersion(source.getPostgresVersion());
    transformation.setPrometheusAutobind(source.getPrometheusAutobind());
    transformation.setResourceProfile(source.getSgInstanceProfile());

    final ClusterPostgresServices sourcePostgresServices = source.getPostgresServices();
    if (sourcePostgresServices != null) {
      transformation.setPostgresServices(new StackGresClusterPostgresServices());
      final StackGresClusterPostgresServices targetPostgresService = transformation
          .getPostgresServices();

      final ClusterPostgresService sourcePrimaryService = sourcePostgresServices.getPrimary();
      if (sourcePrimaryService != null) {
        targetPostgresService.setPrimary(new StackGresClusterPostgresService());
        final StackGresClusterPostgresService targetPrimaryService = targetPostgresService
            .getPrimary();
        targetPrimaryService.setAnnotations(sourcePrimaryService.getAnnotations());
        targetPrimaryService.setType(sourcePrimaryService.getType());
        targetPrimaryService.setEnabled(sourcePrimaryService.getEnabled());
      }

      final ClusterPostgresService sourceReplicaService = sourcePostgresServices.getReplicas();
      if (sourceReplicaService != null) {
        targetPostgresService.setReplicas(new StackGresClusterPostgresService());
        final StackGresClusterPostgresService targetReplicaService = targetPostgresService
            .getReplicas();
        targetReplicaService.setAnnotations(sourceReplicaService.getAnnotations());
        targetReplicaService.setEnabled(sourceReplicaService.getEnabled());
        targetReplicaService.setType(sourceReplicaService.getType());
      }
    }

    final ClusterInitData sourceInitData = source.getInitData();
    if (sourceInitData != null) {

      final StackGresClusterInitData targetInitData = new StackGresClusterInitData();
      transformation.setInitData(targetInitData);

      if (sourceInitData.getRestore() != null) {
        targetInitData.setRestore(
            getCustomResourceRestore(sourceInitData.getRestore()));
      }

      if (sourceInitData.getScripts() != null) {
        targetInitData.setScripts(getCustomResourceScripts(sourceInitData.getScripts()));
      }

    }

    Optional.ofNullable(source.getMetadata())
        .map(ClusterSpecMetadata::getAnnotations)
        .ifPresent(sourceAnnotations -> {
          transformation.setMetadata(new StackGresClusterSpecMetadata());

          final StackGresClusterSpecAnnotations targetAnnotations
              = new StackGresClusterSpecAnnotations();
          transformation.getMetadata().setAnnotations(targetAnnotations);

          if (sourceAnnotations.getAllResources() != null) {
            targetAnnotations.setAllResources(sourceAnnotations.getAllResources());
          }
          if (sourceAnnotations.getPods() != null) {
            targetAnnotations.setPods(sourceAnnotations.getPods());
          }
          if (sourceAnnotations.getServices() != null) {
            targetAnnotations.setServices(sourceAnnotations.getServices());
          }
        });

    final StackGresClusterPod targetPod = new StackGresClusterPod();
    transformation.setPod(targetPod);
    targetPod.setPersistentVolume(new StackGresPodPersistentVolume());
    targetPod.getPersistentVolume().setStorageClass(
        source.getPods().getPersistentVolume().getStorageClass());
    targetPod.getPersistentVolume().setVolumeSize(
        source.getPods().getPersistentVolume().getVolumeSize());

    targetPod
        .setDisableConnectionPooling(source.getPods().getDisableConnectionPooling());
    targetPod
        .setDisableMetricsExporter(source.getPods().getDisableMetricsExporter());
    targetPod
        .setDisablePostgresUtil(source.getPods().getDisablePostgresUtil());

    targetPod.setMetadata(Optional.ofNullable(source.getPods().getMetadata())
        .map(sourcePodMetadata -> {
          StackGresClusterPodMetadata targetMetadata = new StackGresClusterPodMetadata();
          targetMetadata.setLabels(sourcePodMetadata.getLabels());
          return targetMetadata;
        }).orElse(null));

    targetPod.setScheduling(Optional.ofNullable(source.getPods().getScheduling())
        .map(sourceScheduling -> {
          StackGresPodScheduling targetScheduling = new StackGresPodScheduling();
          targetScheduling.setNodeSelector(sourceScheduling.getNodeSelector());
          targetScheduling.setTolerations(sourceScheduling.getTolerations());
          return targetScheduling;
        }).orElse(null));
    transformation.setDistributedLogs(
        getCustomResourceDistributedLogs(source.getDistributedLogs()));

    return transformation;
  }

  private List<StackGresClusterScriptEntry> getCustomResourceScripts(
      List<ClusterScriptEntry> scripts) {
    return scripts.stream().map(entry -> {
      StackGresClusterScriptEntry targetEntry = new StackGresClusterScriptEntry();
      targetEntry.setName(entry.getName());
      targetEntry.setDatabase(entry.getDatabase());
      targetEntry.setScript(entry.getScript());
      if (entry.getScriptFrom() != null) {
        StackGresClusterScriptFrom targetScriptFrom = new StackGresClusterScriptFrom();
        targetScriptFrom.setSecretKeyRef(entry.getScriptFrom().getSecretKeyRef());
        targetScriptFrom.setConfigMapKeyRef(entry.getScriptFrom().getConfigMapKeyRef());
        targetEntry.setScriptFrom(targetScriptFrom);
      }
      return targetEntry;
    }).collect(ImmutableList.toImmutableList());
  }

  private io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction
      getCustomResourceNonProduction(ClusterNonProduction source) {
    if (source == null) {
      return null;
    }
    io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction transformation =
        new io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private StackGresClusterRestore getCustomResourceRestore(
      ClusterRestore source) {
    if (source == null) {
      return null;
    }
    StackGresClusterRestore transformation = new StackGresClusterRestore();
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setBackupUid(source.getBackupUid());
    return transformation;
  }

  private StackGresClusterDistributedLogs getCustomResourceDistributedLogs(
      ClusterDistributedLogs source) {
    if (source == null) {
      return null;
    }
    StackGresClusterDistributedLogs transformation =
        new StackGresClusterDistributedLogs();
    transformation.setDistributedLogs(source.getDistributedLogs());
    return transformation;
  }

  public ClusterSpec getResourceSpec(StackGresClusterSpec source) {
    if (source == null) {
      return null;
    }
    ClusterSpec transformation = new ClusterSpec();
    transformation.setConfigurations(new ClusterConfiguration());
    transformation.getConfigurations().setSgBackupConfig(
        source.getConfiguration().getBackupConfig());
    transformation.getConfigurations().setSgPoolingConfig(source
        .getConfiguration().getConnectionPoolingConfig());
    transformation.setInstances(source.getInstances());
    transformation.setNonProduction(
        getResourceNonProduction(source.getNonProduction()));
    transformation.getConfigurations().setSgPostgresConfig(
        source.getConfiguration().getPostgresConfig());
    transformation.setPostgresVersion(source.getPostgresVersion());
    transformation.setPrometheusAutobind(source.getPrometheusAutobind());
    transformation.setSgInstanceProfile(source.getResourceProfile());

    final StackGresClusterInitData sourceInitData = source.getInitData();
    if (sourceInitData != null) {
      ClusterInitData targetInitData = new ClusterInitData();
      transformation.setInitData(targetInitData);
      final StackGresClusterRestore sourceRestore = sourceInitData.getRestore();
      if (sourceRestore != null) {
        targetInitData.setRestore(getResourceRestore(sourceRestore));
      }

      if (sourceInitData.getScripts() != null) {
        targetInitData.setScripts(sourceInitData.getScripts().stream().map(sourceEntry -> {
          ClusterScriptEntry targetEntry = new ClusterScriptEntry();
          targetEntry.setScript(sourceEntry.getScript());
          targetEntry.setDatabase(sourceEntry.getDatabase());
          targetEntry.setName(sourceEntry.getName());
          if (sourceEntry.getScriptFrom() != null) {
            targetEntry.setScriptFrom(new ClusterScriptFrom());
            targetEntry.getScriptFrom().setSecretKeyRef(
                sourceEntry.getScriptFrom().getSecretKeyRef());
            targetEntry.getScriptFrom().setConfigMapKeyRef(
                sourceEntry.getScriptFrom().getConfigMapKeyRef());
          }
          return targetEntry;
        }).collect(ImmutableList.toImmutableList()));
      }
    }

    final ClusterPod targetPod = new ClusterPod();
    final StackGresClusterPod sourcePod = source.getPod();

    transformation.setPods(targetPod);
    targetPod.setPersistentVolume(new ClusterPodPersistentVolume());
    targetPod.getPersistentVolume().setStorageClass(
        sourcePod.getPersistentVolume().getStorageClass());
    targetPod.getPersistentVolume().setVolumeSize(
        sourcePod.getPersistentVolume().getVolumeSize());
    targetPod
        .setDisableConnectionPooling(sourcePod.getDisableConnectionPooling());
    targetPod
        .setDisableMetricsExporter(sourcePod.getDisableMetricsExporter());
    targetPod
        .setDisablePostgresUtil(sourcePod.getDisablePostgresUtil());

    final StackGresClusterPostgresServices sourcePostgresServices = source.getPostgresServices();
    if (sourcePostgresServices != null) {
      transformation.setPostgresServices(new ClusterPostgresServices());
      final ClusterPostgresServices targetPostgresService = transformation
          .getPostgresServices();

      final StackGresClusterPostgresService sourcePrimaryService = sourcePostgresServices
          .getPrimary();
      if (sourcePrimaryService != null) {
        targetPostgresService.setPrimary(new ClusterPostgresService());
        final ClusterPostgresService targetPrimaryService = targetPostgresService
            .getPrimary();
        targetPrimaryService.setAnnotations(sourcePrimaryService.getAnnotations());
        targetPrimaryService.setType(sourcePrimaryService.getType());
        targetPrimaryService.setEnabled(sourcePrimaryService.getEnabled());
      }

      final StackGresClusterPostgresService sourceReplicaService = sourcePostgresServices
          .getReplicas();
      if (sourceReplicaService != null) {
        targetPostgresService.setReplicas(new ClusterPostgresService());
        final ClusterPostgresService targetReplicaService = targetPostgresService.getReplicas();
        targetReplicaService.setAnnotations(sourceReplicaService.getAnnotations());
        targetReplicaService.setEnabled(sourceReplicaService.getEnabled());
        targetReplicaService.setType(sourceReplicaService.getType());
      }
    }

    Optional.ofNullable(source.getMetadata())
        .map(StackGresClusterSpecMetadata::getAnnotations)
        .ifPresent(sourceAnnotations -> {
          transformation.setMetadata(new ClusterSpecMetadata());

          final ClusterSpecAnnotations targetAnnotations = new ClusterSpecAnnotations();
          transformation.getMetadata().setAnnotations(targetAnnotations);

          if (sourceAnnotations.getAllResources() != null) {
            targetAnnotations.setAllResources(sourceAnnotations.getAllResources());
          }
          if (sourceAnnotations.getPods() != null) {
            targetAnnotations.setPods(sourceAnnotations.getPods());
          }
          if (sourceAnnotations.getServices() != null) {
            targetAnnotations.setServices(sourceAnnotations.getServices());
          }
        });

    targetPod.setMetadata(Optional.ofNullable(sourcePod.getMetadata()).map(sourcePodMetadata -> {
      ClusterPodMetadata clusterPodMetadata = new ClusterPodMetadata();
      clusterPodMetadata.setLabels(sourcePodMetadata.getLabels());
      return clusterPodMetadata;
    }).orElse(null));

    targetPod.setScheduling(Optional.ofNullable(sourcePod.getScheduling())
        .map(sourcePodScheduling -> {
          PodScheduling podScheduling = new PodScheduling();
          podScheduling.setNodeSelector(sourcePodScheduling.getNodeSelector());
          podScheduling.setTolerations(sourcePodScheduling.getTolerations());
          return podScheduling;
        }).orElse(null));

    transformation.setDistributedLogs(
        getResourceDistributedLogs(source.getDistributedLogs()));

    return transformation;
  }

  private ClusterNonProduction getResourceNonProduction(
      io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction source) {
    if (source == null) {
      return null;
    }
    ClusterNonProduction transformation = new ClusterNonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    return transformation;
  }

  private ClusterRestore getResourceRestore(
      StackGresClusterRestore source) {
    if (source == null) {
      return null;
    }
    ClusterRestore transformation = new ClusterRestore();
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setBackupUid(source.getBackupUid());
    return transformation;
  }

  private ClusterDistributedLogs getResourceDistributedLogs(
      StackGresClusterDistributedLogs source) {
    if (source == null) {
      return null;
    }
    ClusterDistributedLogs transformation = new ClusterDistributedLogs();
    transformation.setDistributedLogs(source.getDistributedLogs());
    return transformation;
  }

}
