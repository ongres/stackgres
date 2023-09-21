/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.google.common.io.Resources;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitalData;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServiceBuilder;
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
        .getVersion(POSTGRESQL_VERSION);
  }

  static String getPostgresMajorVersion(StackGresDistributedLogs distributedLogs) {
    return StackGresComponent.POSTGRESQL.get(distributedLogs)
        .getMajorVersion(POSTGRESQL_VERSION);
  }

  static String getPostgresBuildMajorVersion(StackGresDistributedLogs distributedLogs) {
    return StackGresComponent.POSTGRESQL.get(distributedLogs)
        .getBuildMajorVersion(POSTGRESQL_VERSION);
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
    distributedLogsCluster.getSpec().setSgInstanceProfile(
        distributedLogs.getSpec().getSgInstanceProfile());
    if (distributedLogs.getSpec().getConfigurations() != null) {
      distributedLogsCluster.getSpec().setConfigurations(new StackGresClusterConfigurations());
      distributedLogsCluster.getSpec().getConfigurations().setSgPostgresConfig(
          distributedLogs.getSpec().getConfigurations().getSgPostgresConfig());
    }
    distributedLogsCluster.getSpec().setPods(new StackGresClusterPods());
    distributedLogsCluster.getSpec().getPods().setPersistentVolume(
        new StackGresPodPersistentVolume());
    distributedLogsCluster.getSpec().getPods().getPersistentVolume().setSize(
        distributedLogs.getSpec().getPersistentVolume().getSize());
    distributedLogsCluster.getSpec().getPods().getPersistentVolume().setStorageClass(
        distributedLogs.getSpec().getPersistentVolume().getStorageClass());
    distributedLogsCluster.getSpec().setPostgresServices(
        buildClusterPostgresServices(distributedLogs.getSpec()));
    distributedLogsCluster.getSpec().getPods().setScheduling(new StackGresClusterPodScheduling());
    Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getScheduling)
        .ifPresent(distributedLogsScheduling -> {
          distributedLogsCluster.getSpec().getPods().getScheduling().setNodeSelector(
              distributedLogsScheduling.getNodeSelector());
          distributedLogsCluster.getSpec().getPods().getScheduling().setTolerations(
              distributedLogsScheduling.getTolerations());
          distributedLogsCluster.getSpec().getPods().getScheduling().setNodeAffinity(
              distributedLogsScheduling.getNodeAffinity());
          distributedLogsCluster.getSpec().getPods().getScheduling().setPodAffinity(
              distributedLogsScheduling.getPodAffinity());
          distributedLogsCluster.getSpec().getPods().getScheduling().setPodAntiAffinity(
              distributedLogsScheduling.getPodAntiAffinity());
        });
    distributedLogsCluster.getSpec().setInitialData(new StackGresClusterInitalData());
    distributedLogsCluster.getSpec().getInitialData().setScripts(
        List.of(new StackGresClusterScriptEntry()));
    distributedLogsCluster.getSpec().getInitialData().getScripts().get(0).setName(
        "distributed-logs-template");
    distributedLogsCluster.getSpec().getInitialData().getScripts().get(0).setDatabase(
        "template1");
    distributedLogsCluster.getSpec().getInitialData().getScripts().get(0).setScript(
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

  static StackGresClusterPostgresServices buildClusterPostgresServices(
      StackGresDistributedLogsSpec stackGresDistributedLogsSpec) {
    StackGresClusterPostgresServices postgresServices = new StackGresClusterPostgresServices();
    Optional.ofNullable(stackGresDistributedLogsSpec)
        .map(StackGresDistributedLogsSpec::getPostgresServices)
        .ifPresent(pgServices -> {
          postgresServices.setPrimary(
              buildClusterPostgresService(pgServices.getPrimary()));
          postgresServices.setReplicas(
              buildClusterPostgresService(pgServices.getReplicas()));
        });
    return postgresServices;
  }

  static StackGresClusterPostgresService buildClusterPostgresService(
      StackGresPostgresService postgresService) {
    if (postgresService == null) {
      return null;
    }
    return new StackGresClusterPostgresServiceBuilder()
        .withEnabled(postgresService.getEnabled())
        .withType(postgresService.getType())
        .withAllocateLoadBalancerNodePorts(postgresService.getAllocateLoadBalancerNodePorts())
        .withClusterIP(postgresService.getClusterIP())
        .withClusterIPs(postgresService.getClusterIPs())
        .withExternalIPs(postgresService.getExternalIPs())
        .withHealthCheckNodePort(postgresService.getHealthCheckNodePort())
        .withInternalTrafficPolicy(postgresService.getInternalTrafficPolicy())
        .withIpFamilies(postgresService.getIpFamilies())
        .withIpFamilyPolicy(postgresService.getIpFamilyPolicy())
        .withLoadBalancerClass(postgresService.getLoadBalancerClass())
        .withLoadBalancerIP(postgresService.getLoadBalancerIP())
        .withLoadBalancerSourceRanges(postgresService.getLoadBalancerSourceRanges())
        .withSessionAffinity(postgresService.getSessionAffinity())
        .withSessionAffinityConfig(postgresService.getSessionAffinityConfig())
        .build();
  }

}
