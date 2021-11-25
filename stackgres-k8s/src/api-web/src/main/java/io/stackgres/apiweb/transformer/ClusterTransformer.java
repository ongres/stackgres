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
import io.stackgres.apiweb.app.postgres.service.PostgresService;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.apiweb.dto.cluster.ClusterCondition;
import io.stackgres.apiweb.dto.cluster.ClusterConfiguration;
import io.stackgres.apiweb.dto.cluster.ClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.apiweb.dto.cluster.ClusterDbOpsMinorVersionUpgradeStatus;
import io.stackgres.apiweb.dto.cluster.ClusterDbOpsRestartStatus;
import io.stackgres.apiweb.dto.cluster.ClusterDbOpsSecurityUpgradeStatus;
import io.stackgres.apiweb.dto.cluster.ClusterDbOpsStatus;
import io.stackgres.apiweb.dto.cluster.ClusterDistributedLogs;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.cluster.ClusterExtension;
import io.stackgres.apiweb.dto.cluster.ClusterInitData;
import io.stackgres.apiweb.dto.cluster.ClusterInstalledExtension;
import io.stackgres.apiweb.dto.cluster.ClusterNonProduction;
import io.stackgres.apiweb.dto.cluster.ClusterPod;
import io.stackgres.apiweb.dto.cluster.ClusterPodPersistentVolume;
import io.stackgres.apiweb.dto.cluster.ClusterPostgres;
import io.stackgres.apiweb.dto.cluster.ClusterPostgresServices;
import io.stackgres.apiweb.dto.cluster.ClusterRestore;
import io.stackgres.apiweb.dto.cluster.ClusterRestoreFromBackup;
import io.stackgres.apiweb.dto.cluster.ClusterRestorePitr;
import io.stackgres.apiweb.dto.cluster.ClusterScriptEntry;
import io.stackgres.apiweb.dto.cluster.ClusterScriptFrom;
import io.stackgres.apiweb.dto.cluster.ClusterSpec;
import io.stackgres.apiweb.dto.cluster.ClusterSpecAnnotations;
import io.stackgres.apiweb.dto.cluster.ClusterSpecLabels;
import io.stackgres.apiweb.dto.cluster.ClusterSpecMetadata;
import io.stackgres.apiweb.dto.cluster.ClusterSsl;
import io.stackgres.apiweb.dto.cluster.ClusterStatus;
import io.stackgres.apiweb.transformer.converter.cluster.ClusterPodSchedulingConverter;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCondition;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestoreFromBackup;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestorePitr;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  public StackGresCluster toCustomResource(@NotNull ClusterDto source,
      @Nullable StackGresCluster original) {
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
    transformation.setStatus(getResourceStatus(source.getStatus()));
    transformation.setGrafanaEmbedded(isGrafanaEmbeddedEnabled());
    return transformation;
  }

  public ClusterDto toResourceWithPods(@NotNull StackGresCluster source, @Nullable List<Pod> pods) {
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

  private StackGresClusterSpec getCustomResourceSpec(ClusterSpec source) {
    if (source == null) {
      return null;
    }
    StackGresClusterSpec transformation = new StackGresClusterSpec();

    transformation.setPostgres(new StackGresClusterPostgres());
    transformation.getPostgres().setVersion(source.getPostgres().getVersion());
    transformation.getPostgres().setFlavor(source.getPostgres().getFlavor());
    transformation.getPostgres().setExtensions(Optional.ofNullable(
        source.getPostgres().getExtensions())
        .stream()
        .flatMap(List::stream)
        .map(this::getCustomResourceExtension)
        .collect(ImmutableList.toImmutableList()));
    final ClusterSsl sourceClusterSsl = source.getPostgres().getSsl();
    if (sourceClusterSsl != null) {
      transformation.getPostgres().setSsl(new StackGresClusterSsl());
      transformation.getPostgres().getSsl().setEnabled(sourceClusterSsl.getEnabled());
      transformation.getPostgres().getSsl().setCertificateSecretKeySelector(
          sourceClusterSsl.getCertificateSecretKeySelector());
      transformation.getPostgres().getSsl().setPrivateKeySecretKeySelector(
          sourceClusterSsl.getPrivateKeySecretKeySelector());
    }

    final ClusterConfiguration sourceClusterConfiguration = source.getConfigurations();
    if (sourceClusterConfiguration != null) {
      transformation.setConfiguration(new StackGresClusterConfiguration());
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
    transformation.setPrometheusAutobind(source.getPrometheusAutobind());
    transformation.setResourceProfile(source.getSgInstanceProfile());

    final ClusterSpecMetadata specMetadata = source.getMetadata();
    if (specMetadata != null) {
      transformation.setMetadata(new StackGresClusterSpecMetadata());
      final ClusterSpecAnnotations sourceAnnotations = specMetadata.getAnnotations();
      if (sourceAnnotations != null) {
        StackGresClusterSpecAnnotations targetAnnotations = new StackGresClusterSpecAnnotations();
        targetAnnotations.setAllResources(sourceAnnotations.getAllResources());
        targetAnnotations.setClusterPods(sourceAnnotations.getClusterPods());
        targetAnnotations.setServices(sourceAnnotations.getServices());
        targetAnnotations.setPrimaryService(sourceAnnotations.getPrimaryService());
        targetAnnotations.setReplicasService(sourceAnnotations.getReplicasService());
        transformation.getMetadata().setAnnotations(targetAnnotations);
      }
      final ClusterSpecLabels sourceLabels = specMetadata.getLabels();
      if (sourceLabels != null) {
        StackGresClusterSpecLabels targetLabels = new StackGresClusterSpecLabels();
        targetLabels.setClusterPods(sourceLabels.getClusterPods());
        transformation.getMetadata().setLabels(targetLabels);
      }
    }

    final ClusterPostgresServices sourcePostgresServices = source.getPostgresServices();
    if (sourcePostgresServices != null) {
      transformation.setPostgresServices(new StackGresClusterPostgresServices());
      final StackGresClusterPostgresServices targetPostgresService = transformation
          .getPostgresServices();

      final PostgresService sourcePrimaryService = sourcePostgresServices.getPrimary();
      if (sourcePrimaryService != null) {
        targetPostgresService.setPrimary(new StackGresPostgresService());
        final StackGresPostgresService targetPrimaryService = targetPostgresService
            .getPrimary();
        targetPrimaryService.setType(sourcePrimaryService.getType());
        targetPrimaryService.setEnabled(sourcePrimaryService.getEnabled());
        targetPrimaryService.setExternalIPs(sourcePrimaryService.getExternalIPs());
      }

      final PostgresService sourceReplicaService = sourcePostgresServices.getReplicas();
      if (sourceReplicaService != null) {
        targetPostgresService.setReplicas(new StackGresPostgresService());
        final StackGresPostgresService targetReplicaService = targetPostgresService
            .getReplicas();
        targetReplicaService.setEnabled(sourceReplicaService.getEnabled());
        targetReplicaService.setType(sourceReplicaService.getType());
        targetReplicaService.setExternalIPs(sourceReplicaService.getExternalIPs());
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

    final StackGresClusterPod targetPod = new StackGresClusterPod();
    transformation.setPod(targetPod);
    targetPod.setPersistentVolume(new StackGresPodPersistentVolume());
    targetPod.getPersistentVolume().setStorageClass(
        source.getPods().getPersistentVolume().getStorageClass());
    targetPod.getPersistentVolume().setSize(
        source.getPods().getPersistentVolume().getSize());

    targetPod
        .setDisableConnectionPooling(source.getPods().getDisableConnectionPooling());
    targetPod
        .setDisableMetricsExporter(source.getPods().getDisableMetricsExporter());
    targetPod
        .setDisablePostgresUtil(source.getPods().getDisablePostgresUtil());

    targetPod.setScheduling(Optional.ofNullable(source.getPods().getScheduling())
        .map(sourceScheduling -> {
          return new ClusterPodSchedulingConverter().to(sourceScheduling);
        }).orElse(null));
    transformation.setDistributedLogs(
        getCustomResourceDistributedLogs(source.getDistributedLogs()));

    return transformation;
  }

  private StackGresClusterExtension getCustomResourceExtension(ClusterExtension source) {
    StackGresClusterExtension transformation = new StackGresClusterExtension();
    transformation.setName(source.getName());
    transformation.setPublisher(source.getPublisher());
    transformation.setVersion(source.getVersion());
    transformation.setRepository(source.getRepository());
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
    transformation.setEnabledFeatureGates(source.getEnabledFeatureGates());
    return transformation;
  }

  private StackGresClusterRestore getCustomResourceRestore(
      ClusterRestore source) {
    if (source == null) {
      return null;
    }
    StackGresClusterRestore transformation = new StackGresClusterRestore();
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setFromBackup(getCustomResourceRestoreFromBackup(source.getFromBackup()));
    return transformation;
  }

  private StackGresClusterRestoreFromBackup getCustomResourceRestoreFromBackup(
      ClusterRestoreFromBackup source) {
    if (source == null) {
      return null;
    }
    StackGresClusterRestoreFromBackup transformation = new StackGresClusterRestoreFromBackup();
    transformation.setUid(source.getUid());
    transformation.setPointInTimeRecovery(getCustomResourceRestorePitr(
        source.getPointInTimeRecovery()));
    return transformation;
  }

  private StackGresClusterRestorePitr getCustomResourceRestorePitr(
      ClusterRestorePitr source) {
    if (source == null) {
      return null;
    }
    StackGresClusterRestorePitr transformation = new StackGresClusterRestorePitr();
    transformation.setRestoreToTimestamp(source.getRestoreToTimestamp());
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

  private ClusterSpec getResourceSpec(StackGresClusterSpec source) {
    if (source == null) {
      return null;
    }
    ClusterSpec transformation = new ClusterSpec();

    transformation.setPostgres(new ClusterPostgres());
    transformation.getPostgres().setVersion(source.getPostgres().getVersion());
    transformation.getPostgres().setFlavor(source.getPostgres().getFlavor());
    transformation.getPostgres().setExtensions(Optional.ofNullable(
        source.getPostgres().getExtensions())
        .stream()
        .flatMap(List::stream)
        .map(this::getResourceExtension)
        .collect(ImmutableList.toImmutableList()));
    final StackGresClusterSsl sourceClusterSsl = source.getPostgres().getSsl();
    if (sourceClusterSsl != null) {
      transformation.getPostgres().setSsl(new ClusterSsl());
      transformation.getPostgres().getSsl().setEnabled(sourceClusterSsl.getEnabled());
      transformation.getPostgres().getSsl().setCertificateSecretKeySelector(
          sourceClusterSsl.getCertificateSecretKeySelector());
      transformation.getPostgres().getSsl().setPrivateKeySecretKeySelector(
          sourceClusterSsl.getPrivateKeySecretKeySelector());
    }

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
    targetPod.getPersistentVolume().setSize(
        sourcePod.getPersistentVolume().getSize());
    targetPod
        .setDisableConnectionPooling(sourcePod.getDisableConnectionPooling());
    targetPod
        .setDisableMetricsExporter(sourcePod.getDisableMetricsExporter());
    targetPod
        .setDisablePostgresUtil(sourcePod.getDisablePostgresUtil());

    final StackGresClusterSpecMetadata specMetadata = source.getMetadata();
    if (specMetadata != null) {
      transformation.setMetadata(new ClusterSpecMetadata());
      final StackGresClusterSpecAnnotations sourceAnnotations = specMetadata.getAnnotations();
      if (specMetadata.getAnnotations() != null) {
        ClusterSpecAnnotations targetAnnotations = new ClusterSpecAnnotations();
        targetAnnotations.setAllResources(sourceAnnotations.getAllResources());
        targetAnnotations.setClusterPods(sourceAnnotations.getClusterPods());
        targetAnnotations.setServices(sourceAnnotations.getServices());
        targetAnnotations.setPrimaryService(sourceAnnotations.getPrimaryService());
        targetAnnotations.setReplicasService(sourceAnnotations.getReplicasService());
        transformation.getMetadata().setAnnotations(targetAnnotations);
      }
      final StackGresClusterSpecLabels sourceLabels = specMetadata.getLabels();
      if (sourceLabels != null) {
        ClusterSpecLabels targetLabels = new ClusterSpecLabels();
        targetLabels.setClusterPods(sourceLabels.getClusterPods());
        transformation.getMetadata().setLabels(targetLabels);
      }
    }

    final StackGresClusterPostgresServices sourcePostgresServices = source.getPostgresServices();
    if (sourcePostgresServices != null) {
      transformation.setPostgresServices(new ClusterPostgresServices());
      final ClusterPostgresServices targetPostgresService = transformation
          .getPostgresServices();

      final StackGresPostgresService sourcePrimaryService = sourcePostgresServices
          .getPrimary();
      if (sourcePrimaryService != null) {
        targetPostgresService.setPrimary(new PostgresService());
        targetPostgresService.getPrimary().setType(sourcePrimaryService.getType());
        targetPostgresService.getPrimary().setEnabled(sourcePrimaryService.getEnabled());
        targetPostgresService.getPrimary().setExternalIPs(sourcePrimaryService.getExternalIPs());
      }

      final StackGresPostgresService sourceReplicaService = sourcePostgresServices
          .getReplicas();
      if (sourceReplicaService != null) {
        targetPostgresService.setReplicas(new PostgresService());
        targetPostgresService.getReplicas().setEnabled(sourceReplicaService.getEnabled());
        targetPostgresService.getReplicas().setType(sourceReplicaService.getType());
        targetPostgresService.getReplicas().setExternalIPs(sourceReplicaService.getExternalIPs());
      }
    }

    targetPod.setScheduling(Optional.ofNullable(sourcePod.getScheduling())
        .map(sourcePodScheduling -> {
          return new ClusterPodSchedulingConverter().from(sourcePodScheduling);
        }).orElse(null));

    transformation.setDistributedLogs(
        getResourceDistributedLogs(source.getDistributedLogs()));
    if (source.getToInstallPostgresExtensions() != null) {
      transformation.setToInstallPostgresExtensions(source.getToInstallPostgresExtensions().stream()
          .map(this::getClusterInstalledExtension).collect(ImmutableList.toImmutableList()));
    }

    return transformation;
  }

  private ClusterExtension getResourceExtension(StackGresClusterExtension source) {
    ClusterExtension transformation = new ClusterExtension();
    transformation.setName(source.getName());
    transformation.setPublisher(source.getPublisher());
    transformation.setVersion(source.getVersion());
    transformation.setRepository(source.getRepository());
    return transformation;
  }

  private ClusterNonProduction getResourceNonProduction(
      io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction source) {
    if (source == null) {
      return null;
    }
    ClusterNonProduction transformation = new ClusterNonProduction();
    transformation.setDisableClusterPodAntiAffinity(source.getDisableClusterPodAntiAffinity());
    transformation.setEnabledFeatureGates(source.getEnabledFeatureGates());
    return transformation;
  }

  private ClusterRestore getResourceRestore(
      StackGresClusterRestore source) {
    if (source == null) {
      return null;
    }
    ClusterRestore transformation = new ClusterRestore();
    transformation.setDownloadDiskConcurrency(source.getDownloadDiskConcurrency());
    transformation.setFromBackup(getResourceRestoreFromBackup(source.getFromBackup()));
    return transformation;
  }

  private ClusterRestoreFromBackup getResourceRestoreFromBackup(
      StackGresClusterRestoreFromBackup source) {
    if (source == null) {
      return null;
    }
    ClusterRestoreFromBackup transformation = new ClusterRestoreFromBackup();
    transformation.setUid(source.getUid());
    transformation.setPointInTimeRecovery(getResourceRestorePitr(source.getPointInTimeRecovery()));
    return transformation;
  }

  private ClusterRestorePitr getResourceRestorePitr(
      StackGresClusterRestorePitr source) {
    if (source == null) {
      return null;
    }
    ClusterRestorePitr transformation = new ClusterRestorePitr();
    transformation.setRestoreToTimestamp(source.getRestoreToTimestamp());
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

  private ClusterStatus getResourceStatus(StackGresClusterStatus source) {
    if (source == null) {
      return null;
    }
    ClusterStatus transformation = new ClusterStatus();

    final List<StackGresClusterCondition> sourceClusterConditions = source.getConditions();

    if (sourceClusterConditions != null) {
      transformation.setConditions(sourceClusterConditions.stream()
          .map(this::getResourceCondition)
          .collect(ImmutableList.toImmutableList()));
    }

    transformation.setDbOps(getDbOpsStatus(source.getDbOps()));

    return transformation;
  }

  private ClusterCondition getResourceCondition(
      StackGresClusterCondition source) {
    ClusterCondition transformation = new ClusterCondition();
    transformation.setType(source.getType());
    transformation.setStatus(source.getStatus());
    transformation.setReason(source.getReason());
    transformation.setLastTransitionTime(source.getLastTransitionTime());
    transformation.setMessage(source.getMessage());
    return transformation;
  }

  private ClusterDbOpsStatus getDbOpsStatus(StackGresClusterDbOpsStatus source) {
    if (source == null) {
      return null;
    }
    ClusterDbOpsStatus transformation = new ClusterDbOpsStatus();
    if (source.getRestart() != null) {
      ClusterDbOpsRestartStatus transformationRestart =
          new ClusterDbOpsRestartStatus();
      transformationRestart.setInitialInstances(source.getRestart().getInitialInstances());
      transformationRestart.setPrimaryInstance(source.getRestart().getPrimaryInstance());
      transformation.setRestart(transformationRestart);
    }
    if (source.getMinorVersionUpgrade() != null) {
      ClusterDbOpsMinorVersionUpgradeStatus transformationMinorVersionUpgrade =
          new ClusterDbOpsMinorVersionUpgradeStatus();
      transformationMinorVersionUpgrade.setInitialInstances(
          source.getMinorVersionUpgrade().getInitialInstances());
      transformationMinorVersionUpgrade.setPrimaryInstance(
          source.getMinorVersionUpgrade().getPrimaryInstance());
      transformation.setMinorVersionUpgrade(transformationMinorVersionUpgrade);
    }
    if (source.getSecurityUpgrade() != null) {
      ClusterDbOpsSecurityUpgradeStatus transformationSecurityUpgrade =
          new ClusterDbOpsSecurityUpgradeStatus();
      transformationSecurityUpgrade.setInitialInstances(
          source.getSecurityUpgrade().getInitialInstances());
      transformationSecurityUpgrade.setPrimaryInstance(
          source.getSecurityUpgrade().getPrimaryInstance());
      transformation.setSecurityUpgrade(transformationSecurityUpgrade);
    }
    if (source.getMajorVersionUpgrade() != null) {
      ClusterDbOpsMajorVersionUpgradeStatus transformationMajorVersionUpgrade =
          new ClusterDbOpsMajorVersionUpgradeStatus();
      transformationMajorVersionUpgrade.setInitialInstances(
          source.getMajorVersionUpgrade().getInitialInstances());
      transformationMajorVersionUpgrade.setPrimaryInstance(
          source.getMajorVersionUpgrade().getPrimaryInstance());
      transformationMajorVersionUpgrade.setSourcePostgresVersion(
          source.getMajorVersionUpgrade().getSourcePostgresVersion());
      transformationMajorVersionUpgrade.setTargetPostgresVersion(
          source.getMajorVersionUpgrade().getTargetPostgresVersion());
      transformationMajorVersionUpgrade.setCheck(
          source.getMajorVersionUpgrade().getCheck());
      transformationMajorVersionUpgrade.setLink(
          source.getMajorVersionUpgrade().getLink());
      transformationMajorVersionUpgrade.setClone(
          source.getMajorVersionUpgrade().getClone());
      transformationMajorVersionUpgrade.setLocale(
          source.getMajorVersionUpgrade().getLocale());
      transformationMajorVersionUpgrade.setEncoding(
          source.getMajorVersionUpgrade().getEncoding());
      transformationMajorVersionUpgrade.setDataChecksum(
          source.getMajorVersionUpgrade().getDataChecksum());
      transformation.setMajorVersionUpgrade(transformationMajorVersionUpgrade);
    }
    return transformation;
  }

  private ClusterInstalledExtension getClusterInstalledExtension(
      StackGresClusterInstalledExtension source) {
    if (source == null) {
      return null;
    }
    ClusterInstalledExtension transformation = new ClusterInstalledExtension();
    transformation.setName(source.getName());
    transformation.setPublisher(source.getPublisher());
    transformation.setRepository(source.getRepository());
    transformation.setVersion(source.getVersion());
    transformation.setPostgresVersion(source.getPostgresVersion());
    transformation.setBuild(source.getBuild());
    transformation.setExtraMounts(source.getExtraMounts());
    return transformation;
  }

}
