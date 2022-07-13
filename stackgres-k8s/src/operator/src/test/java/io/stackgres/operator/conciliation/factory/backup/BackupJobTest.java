/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import static org.junit.Assert.assertEquals;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupJobTestCase;
import org.junit.jupiter.api.Test;

public class BackupJobTest extends BackupJobTestCase {

  @Test
  public void shouldCreateNewBackupJobWithNodeAffinity_OnceClusterSchedulingBackupHasAffinity() {
    givenExpectedBackupConfigAndClusterValues();
    Stream<HasMetadata> generatedResources = backupJob.generateResource(backupContext);

    var job = (Job) generatedResources.iterator().next();
    assertEquals(1, job.getSpec().getTemplate().getSpec().getAffinity().getNodeAffinity()
        .getPreferredDuringSchedulingIgnoredDuringExecution().size());
    assertEquals(1, job.getSpec().getTemplate().getSpec().getAffinity().getNodeAffinity()
        .getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms().size());

  }

  @Test
  public void shouldCreateNewBackupJobWithNodeSelector_OnceClusterSchedulingBackupHasSelectors() {
    givenExpectedBackupConfigAndClusterValues();
    Stream<HasMetadata> generatedResources = backupJob.generateResource(backupContext);
    var job = (Job) generatedResources.iterator().next();
    assertEquals(2, job.getSpec().getTemplate().getSpec().getNodeSelector().size());
  }

}
