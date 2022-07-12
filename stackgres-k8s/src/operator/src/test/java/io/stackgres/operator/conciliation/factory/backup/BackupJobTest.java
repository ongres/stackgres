/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.LabelFactoryForBackup;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.LabelMapperForCluster;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupPerformance;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactoryDiscoverer;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BackupJobTest {

  @Mock
  private ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext> clusterEnvVarDiscoverer;
  @Mock
  private LabelFactoryForBackup labelFactory;
  @Mock
  private LabelFactoryForCluster<StackGresCluster> labelFactoryForCluster;
  @Mock
  private LabelMapperForCluster<StackGresCluster> labelMapper;
  @Mock
  private ResourceFactory<StackGresBackupContext, PodSecurityContext> podSecurityFactory;
  @Mock
  private KubectlUtil kubectl;
  @Mock
  private StackGresBackupContext context;
  private BackupJob backupJob;
  private StackGresBackup sgBackup;
  private StackGresCluster sgCluster;
  private BackupPerformance backupPerformance;
  private BackupConfiguration backupConfig;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.backupJob = new BackupJob(clusterEnvVarDiscoverer,
        labelFactory, labelFactoryForCluster, podSecurityFactory, kubectl);
    sgBackup = JsonUtil.readFromJson("backup/default.json", StackGresBackup.class);
    sgCluster =
        JsonUtil.readFromJson("stackgres_cluster/scheduling_backup.json", StackGresCluster.class);

    backupPerformance = new BackupPerformance(10L, 10L, 1);
    backupConfig =
        new BackupConfiguration(5, "* * * 5 *", "10", "/tmp", backupPerformance);
    sgBackup.getSpec().setSgCluster(sgCluster.getMetadata().getName());
  }

  @Test
  public void shouldCreateNewBackupJobWithNodeAffinity_OnceClusterSchedulingBackupHasAffinity() {
    givenExpectedBackupConfigAndClusterValues();
    Stream<HasMetadata> generatedResources = backupJob.generateResource(context);

    var job = (Job) generatedResources.iterator().next();
    assertEquals(job.getSpec().getTemplate().getSpec().getAffinity().getNodeAffinity()
        .getPreferredDuringSchedulingIgnoredDuringExecution().size(), 1);
    assertEquals(job.getSpec().getTemplate().getSpec().getAffinity().getNodeAffinity()
        .getRequiredDuringSchedulingIgnoredDuringExecution().getNodeSelectorTerms().size(), 1);

  }

  @Test
  public void shouldCreateNewBackupJobWithNodeSelector_OnceClusterSchedulingBackupHasSelectors() {
    givenExpectedBackupConfigAndClusterValues();
    Stream<HasMetadata> generatedResources = backupJob.generateResource(context);
    var job = (Job) generatedResources.iterator().next();
    assertEquals(2, job.getSpec().getTemplate().getSpec().getNodeSelector().size());
  }

  private void givenExpectedBackupConfigAndClusterValues() {
    given(context.getSource()).willReturn(sgBackup);
    given(context.getCluster()).willReturn(sgCluster);
    given(context.getObjectStorage()).willReturn(Optional.of(new StackGresObjectStorage()));
    given(labelFactoryForCluster.labelMapper()).willReturn(labelMapper);
    given(kubectl.getImageName(sgCluster)).willReturn(StringUtil.generateRandom());
    given(context.getBackupConfiguration()).willReturn(backupConfig);
  }

}
