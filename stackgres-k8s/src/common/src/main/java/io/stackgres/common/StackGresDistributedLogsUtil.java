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
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import org.jooq.lambda.Unchecked;

public interface StackGresDistributedLogsUtil {

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
    spec.setPostgresVersion(StackGresComponent.POSTGRESQL.findVersion(
        StackGresComponent.LATEST));
    spec.setInstances(1);
    final StackGresClusterPod pod = new StackGresClusterPod();
    final StackGresPodPersistentVolume persistentVolume = new StackGresPodPersistentVolume();
    persistentVolume.setVolumeSize(
        distributedLogs.getSpec().getPersistentVolume().getVolumeSize());
    persistentVolume.setStorageClass(
        distributedLogs.getSpec().getPersistentVolume().getStorageClass());
    pod.setPersistentVolume(persistentVolume);
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
    distributedLogsCluster.setSpec(spec);
    return distributedLogsCluster;
  }

}
