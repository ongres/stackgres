/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import static io.stackgres.common.StringUtil.generateRandom;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForBackup;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelMapperForCluster;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupPerformance;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactoryDiscoverer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BackupJobTest {

  @Mock
  private ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext> envFactoryDiscoverer;
  @Mock
  private LabelFactoryForCluster<StackGresCluster> labelFactory;
  @Mock
  private LabelFactoryForBackup backupLabelFactory;
  @Mock
  private ResourceFactory<StackGresBackupContext, PodSecurityContext> backupPodSecurityFactory;
  @Mock
  private KubectlUtil kubectl;
  @Mock
  private StackGresClusterContext clusterContext;
  @Mock
  private StackGresBackupContext backupContext;
  @Mock
  private LabelMapperForCluster<StackGresCluster> labelMapperSgCluster;
  @Mock
  private BackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts;
  @Mock
  private BackupTemplatesVolumeFactory backupTemplatesVolumeFactory;
  private BackupJob backupJob;
  private StackGresCluster sgCluster;
  private StackGresBackup sgBackup;
  private BackupConfiguration backupConfig;
  private BackupPerformance backupPerformance;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    backupJob =
        new BackupJob(envFactoryDiscoverer, backupLabelFactory, labelFactory,
            backupPodSecurityFactory, kubectl, backupScriptTemplatesVolumeMounts,
            backupTemplatesVolumeFactory);
    sgBackup = Fixtures.backup().loadDefault().get();
    sgCluster = Fixtures.cluster().loadSchedulingBackup().get();
    backupPerformance = new BackupPerformance(10L, 10L, 1, null, null);
    backupConfig =
        new BackupConfiguration(5, "* * * 5 *", "10", "/tmp", backupPerformance);
    sgBackup.getSpec().setSgCluster(sgCluster.getMetadata().getName());
  }

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
