/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import org.junit.jupiter.api.Test;

public class BackupCronJobTest extends BackupJobTestCase {

  @Test
  public void shouldCreateNewBackupJobWithNodeSelector_OnceClusterSchedulingBackupHasSelectors() {
    givenExpectedBackupConfigAndClusterValues();
    Stream<HasMetadata> generatedResources = backupCronJob.generateResource(clusterContext);
    var backupCronJob = (CronJob) generatedResources.iterator().next();
    assertEquals(2, backupCronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec()
        .getNodeSelector().size());
  }

  @Test
  public void shouldCreateBackupCronJobWithNodeAffinity_OnceClusterSchedulingBackupHasAffinity() {
    givenExpectedBackupConfigAndClusterValues();
    Stream<HasMetadata> generateResource = backupCronJob.generateResource(clusterContext);
    var job = (CronJob) generateResource.iterator().next();
    var affinity = job.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getAffinity();
    var affinityPreferred =
        affinity.getNodeAffinity().getPreferredDuringSchedulingIgnoredDuringExecution();
    var affinityRequired =
        affinity.getNodeAffinity().getRequiredDuringSchedulingIgnoredDuringExecution();
    assertEquals(1, affinityPreferred.size());
    assertEquals(1, affinityRequired.getNodeSelectorTerms().size());
  }

}
