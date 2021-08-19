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
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodScheduling;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpecMetadata;
import org.jooq.lambda.Unchecked;

public interface StackGresDistributedLogsUtil {

  String POSTGRESQL_VERSION = "12";

  static String getPostgresVersion() {
    return StackGresComponent.POSTGRESQL.findVersion(POSTGRESQL_VERSION);
  }

  static String getPostgresMajorVersion() {
    return StackGresComponent.POSTGRESQL.findMajorVersion(POSTGRESQL_VERSION);
  }

  static String getPostgresBuildMajorVersion() {
    return StackGresComponent.POSTGRESQL.findBuildMajorVersion(POSTGRESQL_VERSION);
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
    final StackGresClusterSpec spec = new StackGresClusterSpec();
    spec.setPostgres(new StackGresClusterPostgres());
    spec.getPostgres().setVersion(getPostgresVersion());
    spec.setInstances(1);
    final StackGresClusterPod pod = new StackGresClusterPod();
    final StackGresPodPersistentVolume persistentVolume = new StackGresPodPersistentVolume();
    persistentVolume.setSize(
        distributedLogs.getSpec().getPersistentVolume().getSize());
    persistentVolume.setStorageClass(
        distributedLogs.getSpec().getPersistentVolume().getStorageClass());
    pod.setPersistentVolume(persistentVolume);
    StackGresClusterPodScheduling scheduling = new StackGresClusterPodScheduling();
    Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getScheduling)
        .ifPresent(distributedLogsScheduling -> {
          scheduling.setNodeSelector(distributedLogsScheduling.getNodeSelector());
          scheduling.setTolerations(distributedLogsScheduling.getTolerations());
        });
    pod.setScheduling(scheduling);
    spec.setPod(pod);
    final StackGresClusterInitData initData = new StackGresClusterInitData();
    final StackGresClusterScriptEntry script = new StackGresClusterScriptEntry();
    script.setName("distributed-logs-template");
    script.setDatabase("template1");
    script.setScript(Unchecked.supplier(() -> Resources
          .asCharSource(StackGresDistributedLogsUtil.class.getResource(
              "/distributed-logs-template.sql"),
              StandardCharsets.UTF_8)
          .read()).get());
    initData.setScripts(ImmutableList.of(script));
    spec.setInitData(initData);
    final StackGresClusterNonProduction nonProduction = new StackGresClusterNonProduction();
    nonProduction.setDisableClusterPodAntiAffinity(
        Optional.ofNullable(distributedLogs.getSpec().getNonProduction())
        .map(StackGresDistributedLogsNonProduction::getDisableClusterPodAntiAffinity)
        .orElse(false));
    spec.setNonProduction(nonProduction);
    final StackGresClusterSpecMetadata metadata = new StackGresClusterSpecMetadata();
    final StackGresClusterSpecAnnotations annotations = new StackGresClusterSpecAnnotations();
    Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getMetadata)
        .map(StackGresDistributedLogsSpecMetadata::getAnnotations)
        .ifPresent(distributedLogsAnnotations -> {
          annotations.setAllResources(distributedLogsAnnotations.getAllResources());
          annotations.setClusterPods(distributedLogsAnnotations.getPods());
          annotations.setPrimaryService(distributedLogsAnnotations.getServices());
          annotations.setReplicasService(distributedLogsAnnotations.getServices());
        });
    metadata.setAnnotations(annotations);
    spec.setMetadata(metadata);
    spec.setToInstallPostgresExtensions(
        Optional.ofNullable(distributedLogs.getSpec())
        .map(StackGresDistributedLogsSpec::getToInstallPostgresExtensions)
        .orElse(null));
    distributedLogsCluster.setSpec(spec);
    return distributedLogsCluster;
  }

}
