/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import static io.stackgres.common.StringUtil.generateRandom;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelMapperForCluster;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupPerformance;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.cluster.ClusterEnvironmentVariablesFactoryDiscoverer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BackupCronJobTest {

  @Mock
  private LabelFactoryForCluster<StackGresCluster> labelFactory;
  @Mock
  private ResourceFactory<StackGresClusterContext, PodSecurityContext> clusterPodSecurityFactory;
  @Mock
  private KubectlUtil kubectl;
  @Mock
  private StackGresClusterContext clusterContext;
  @Mock
  private StackGresBackupContext backupContext;
  @Mock
  private LabelMapperForCluster<StackGresCluster> labelMapperSgCluster;
  @Mock
  private ClusterEnvironmentVariablesFactoryDiscoverer envFactoryDiscoverer;
  @Mock
  private BackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts;
  @Mock
  private BackupTemplatesVolumeFactory backupTemplatesConfigMap;
  private BackupCronJob backupCronJob;
  private StackGresCluster sgCluster;
  private StackGresBackup sgBackup;
  private BackupConfiguration backupConfig;
  private BackupPerformance backupPerformance;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    backupCronJob =
        new BackupCronJob(labelFactory, clusterPodSecurityFactory,
            kubectl, envFactoryDiscoverer,
            backupScriptTemplatesVolumeMounts, backupTemplatesConfigMap);
    sgBackup = Fixtures.backup().loadDefault().get();
    sgCluster = Fixtures.cluster().loadSchedulingBackup().get();
    backupPerformance = new BackupPerformance(10L, 10L, 1, null, null);
    backupConfig =
        new BackupConfiguration(5, "* * * 5 *", "10", "/tmp", backupPerformance,
            null, null, null, null, null, null, null);
    sgBackup.getSpec().setSgCluster(sgCluster.getMetadata().getName());
  }

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

  private void givenExpectedBackupConfigAndClusterValues() {
    given(clusterContext.getBackupConfiguration()).willReturn(Optional.of(backupConfig));
    given(clusterContext.getSource()).willReturn(sgCluster);
    given(clusterContext.getCluster()).willReturn(sgCluster);
    given(clusterContext.getBackupConfigurationCustomResourceName())
        .willReturn(Optional.of(generateRandom()));

    given(backupContext.getBackupConfiguration()).willReturn(backupConfig);
    given(backupContext.getSource()).willReturn(sgBackup);
    given(backupContext.getCluster()).willReturn(sgCluster);
    given(labelFactory.labelMapper()).willReturn(labelMapperSgCluster);
    given(backupContext.getObjectStorage()).willReturn(Optional.of(new StackGresObjectStorage()));

    given(kubectl.getImageName(sgCluster)).willReturn(generateRandom());
  }

}
