/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import static io.stackgres.common.StringUtil.generateRandom;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.LabelFactoryForBackup;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.LabelMapperForCluster;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.backup.BackupConfiguration;
import io.stackgres.operator.conciliation.backup.BackupPerformance;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.backup.BackupJob;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterEnvironmentVariablesFactoryDiscoverer;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BackupJobTestCase {

  @Mock
  protected ClusterEnvironmentVariablesFactoryDiscoverer<ClusterContext> envFactoryDiscoverer;
  @Mock
  protected LabelFactoryForCluster<StackGresCluster> labelFactory;
  @Mock
  protected LabelFactoryForBackup backupLabelFactory;
  @Mock
  protected ResourceFactory<StackGresClusterContext, PodSecurityContext> clusterPodSecurityFactory;
  @Mock
  protected ResourceFactory<StackGresBackupContext, PodSecurityContext> backupPodSecurityFactory;
  @Mock
  protected KubectlUtil kubectl;
  @Mock
  protected StackGresClusterContext clusterContext;
  @Mock
  protected StackGresBackupContext backupContext;
  @Mock
  protected LabelMapperForCluster<StackGresCluster> labelMapperSgCluster;
  protected BackupCronJob backupCronJob;
  protected BackupJob backupJob;
  protected StackGresCluster sgCluster;
  protected StackGresBackup sgBackup;
  protected BackupConfiguration backupConfig;
  protected BackupPerformance backupPerformance;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    backupCronJob =
        new BackupCronJob(envFactoryDiscoverer, labelFactory, clusterPodSecurityFactory,
            kubectl);
    backupJob = new BackupJob(envFactoryDiscoverer, backupLabelFactory, labelFactory,
        backupPodSecurityFactory, kubectl);
    sgBackup = Fixtures.backup().loadDefault().get();
    sgCluster = Fixtures.cluster().loadSchedulingBackup().get();
    backupPerformance = new BackupPerformance(10L, 10L, 1, null, null);
    backupConfig =
        new BackupConfiguration(5, "* * * 5 *", "10", "/tmp", backupPerformance);
    sgBackup.getSpec().setSgCluster(sgCluster.getMetadata().getName());

  }

  protected void givenExpectedBackupConfigAndClusterValues() {
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
