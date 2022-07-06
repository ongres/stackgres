/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecMetadata;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;

public interface StackGresDistributedLogsUtil {

  String POSTGRESQL_VERSION = "12";

  static String getPostgresVersion(StackGresDistributedLogs distributedLogs) {
    return StackGresComponent.POSTGRESQL.get(distributedLogs)
        .findVersion(POSTGRESQL_VERSION);
  }

  static String getPostgresMajorVersion(StackGresDistributedLogs distributedLogs) {
    return StackGresComponent.POSTGRESQL.get(distributedLogs)
        .findMajorVersion(POSTGRESQL_VERSION);
  }

  static String getPostgresBuildMajorVersion(StackGresDistributedLogs distributedLogs) {
    return StackGresComponent.POSTGRESQL.get(distributedLogs)
        .findBuildMajorVersion(POSTGRESQL_VERSION);
  }

  static @NotNull StackGresComponent getPostgresFlavorComponent(
      StackGresDistributedLogs distribtuedLogs) {
    return StackGresComponent.POSTGRESQL;
  }

  static StackGresCluster getStackGresClusterForDistributedLogs(
      StackGresDistributedLogs distributedLogs) {
    final StackGresCluster distributedLogsCluster = new StackGresCluster();
    distributedLogsCluster.getMetadata().setNamespace(
        distributedLogs.getMetadata().getNamespace());
    distributedLogsCluster.getMetadata().setName(
        distributedLogs.getMetadata().getName());
    distributedLogsCluster.getMetadata().setUid(
        distributedLogs.getMetadata().getUid());
    distributedLogsCluster.setSpec(new StackGresClusterSpec());
    distributedLogsCluster.getSpec().setPostgres(new StackGresClusterPostgres());
    distributedLogsCluster.getSpec().getPostgres().setVersion(
        getPostgresVersion(distributedLogs));
    distributedLogsCluster.getSpec().setInstances(1);
    distributedLogsCluster.getSpec().setResourceProfile(
        distributedLogs.getSpec().getResourceProfile());
    if (distributedLogs.getSpec().getConfiguration() != null) {
      distributedLogsCluster.getSpec().setConfiguration(new StackGresClusterConfiguration());
      distributedLogsCluster.getSpec().getConfiguration().setPostgresConfig(
          distributedLogs.getSpec().getConfiguration().getPostgresConfig());
    }
    distributedLogsCluster.getSpec().setPod(new StackGresClusterPod());
    distributedLogsCluster.getSpec().getPod().setPersistentVolume(
        new StackGresPodPersistentVolume());
    distributedLogsCluster.getSpec().getPod().getPersistentVolume().setSize(
        distributedLogs.getSpec().getPersistentVolume().getSize());
    distributedLogsCluster.getSpec().getPod().getPersistentVolume().setStorageClass(
        distributedLogs.getSpec().getPersistentVolume().getStorageClass());
    distributedLogsCluster.getSpec().setPostgresServices(
        buildPostgresServices(distributedLogs.getSpec()));
    distributedLogsCluster.getSpec().getPod().setScheduling(new StackGresClusterPodScheduling());
    Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getScheduling)
        .ifPresent(distributedLogsScheduling -> {
          distributedLogsCluster.getSpec().getPod().getScheduling().setNodeSelector(
              distributedLogsScheduling.getNodeSelector());
          distributedLogsCluster.getSpec().getPod().getScheduling().setTolerations(
              distributedLogsScheduling.getTolerations());
        });
    distributedLogsCluster.getSpec().setInitData(new StackGresClusterInitData());
    distributedLogsCluster.getSpec().getInitData().setScripts(
        ImmutableList.of(new StackGresClusterScriptEntry()));
    distributedLogsCluster.getSpec().getInitData().getScripts().get(0).setName(
        "distributed-logs-template");
    distributedLogsCluster.getSpec().getInitData().getScripts().get(0).setDatabase(
        "template1");
    distributedLogsCluster.getSpec().getInitData().getScripts().get(0).setScript(
        Unchecked.supplier(() -> Resources
            .asCharSource(StackGresDistributedLogsUtil.class.getResource(
                "/distributed-logs-template.sql"),
                StandardCharsets.UTF_8)
            .read()).get());
    distributedLogsCluster.getSpec().setNonProductionOptions(new StackGresClusterNonProduction());
    distributedLogsCluster.getSpec().getNonProductionOptions().setDisableClusterPodAntiAffinity(
        Optional.ofNullable(distributedLogs.getSpec().getNonProductionOptions())
            .map(StackGresDistributedLogsNonProduction::getDisableClusterPodAntiAffinity)
            .orElse(false));
    distributedLogsCluster.getSpec().setMetadata(new StackGresClusterSpecMetadata());
    distributedLogsCluster.getSpec().getMetadata().setAnnotations(
        new StackGresClusterSpecAnnotations());
    Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresDistributedLogsSpecMetadata::getAnnotations)
        .ifPresent(distributedLogsAnnotations -> {
          distributedLogsCluster.getSpec().getMetadata().getAnnotations().setAllResources(
              distributedLogsAnnotations.getAllResources());
          distributedLogsCluster.getSpec().getMetadata().getAnnotations().setClusterPods(
              distributedLogsAnnotations.getPods());
          distributedLogsCluster.getSpec().getMetadata().getAnnotations().setPrimaryService(
              distributedLogsAnnotations.getServices());
          distributedLogsCluster.getSpec().getMetadata().getAnnotations().setReplicasService(
              distributedLogsAnnotations.getServices());
        });
    distributedLogsCluster.getSpec().setToInstallPostgresExtensions(
        Optional.ofNullable(distributedLogs.getSpec())
        .map(StackGresDistributedLogsSpec::getToInstallPostgresExtensions)
        .orElse(null));
    distributedLogsCluster.setStatus(new StackGresClusterStatus());
    if (distributedLogs.getStatus() != null) {
      distributedLogsCluster.getStatus().setArch(distributedLogs.getStatus().getArch());
      distributedLogsCluster.getStatus().setOs(distributedLogs.getStatus().getOs());
    }
    return distributedLogsCluster;
  }

  static StackGresClusterPostgresServices buildPostgresServices(
      StackGresDistributedLogsSpec stackGresDistributedLogsSpec) {
    StackGresClusterPostgresServices postgresServices = new StackGresClusterPostgresServices();
    Optional.ofNullable(stackGresDistributedLogsSpec)
        .map(StackGresDistributedLogsSpec::getPostgresServices)
        .ifPresent(pgServices -> {
          postgresServices.setPrimary(pgServices.getPrimary());
          postgresServices.setReplicas(pgServices.getReplicas());
        });
    return postgresServices;
  }

}
