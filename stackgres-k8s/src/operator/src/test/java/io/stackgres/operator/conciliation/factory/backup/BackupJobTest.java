/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import static io.stackgres.common.StringUtil.generateRandom;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.Toleration;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsSchedulingBackup;
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
import io.stackgres.operator.conciliation.factory.cluster.ClusterEnvironmentVariablesFactoryDiscoverer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BackupJobTest {

  @Mock
  private LabelFactoryForCluster labelFactory;
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
  private LabelMapperForCluster labelMapperSgCluster;
  @Mock
  private ClusterEnvironmentVariablesFactoryDiscoverer envFactoryDiscoverer;
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
        new BackupJob(backupLabelFactory, labelFactory,
            backupPodSecurityFactory, kubectl, envFactoryDiscoverer,
            backupScriptTemplatesVolumeMounts, backupTemplatesVolumeFactory);
    sgBackup = Fixtures.backup().loadDefault().get();
    sgCluster = Fixtures.cluster().loadSchedulingBackup().get();
    backupPerformance = new BackupPerformance(10L, 10L, 1, null, null);
    backupConfig =
        new BackupConfiguration(5, "* * * 5 *", "10", "/tmp", backupPerformance,
            null, null, null, null, null, null, null);
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

  @Test
  public void generateResource_whenTolerationsSet_shouldApplyToJob() {
    Toleration toleration = new Toleration("NoSchedule", "key1", "Equal", null, "value1");
    StackGresClusterPodsSchedulingBackup backupScheduling =
        new StackGresClusterPodsSchedulingBackup();
    backupScheduling.setTolerations(List.of(toleration));
    sgCluster.getSpec().getPods().getScheduling().setBackup(backupScheduling);

    givenExpectedBackupConfigAndClusterValues();
    given(backupContext.getClusterBackupNamespaces()).willReturn(Set.of());

    Stream<HasMetadata> generatedResources = backupJob.generateResource(backupContext);
    var job = (Job) generatedResources.iterator().next();

    assertNotNull(job.getSpec().getTemplate().getSpec().getTolerations());
    assertEquals(1, job.getSpec().getTemplate().getSpec().getTolerations().size());
    assertEquals("key1",
        job.getSpec().getTemplate().getSpec().getTolerations().get(0).getKey());
    assertEquals("NoSchedule",
        job.getSpec().getTemplate().getSpec().getTolerations().get(0).getEffect());
    assertEquals("value1",
        job.getSpec().getTemplate().getSpec().getTolerations().get(0).getValue());
  }

  @Test
  public void generateResource_whenPerformanceSettings_shouldSetEnvVars() {
    givenExpectedBackupConfigAndClusterValues();
    given(backupContext.getClusterBackupNamespaces()).willReturn(Set.of());

    Stream<HasMetadata> generatedResources = backupJob.generateResource(backupContext);
    var job = (Job) generatedResources.iterator().next();

    var container = job.getSpec().getTemplate().getSpec().getContainers().get(0);
    assertTrue("Expected COMPRESSION env var",
        container.getEnv().stream()
            .anyMatch(e -> "COMPRESSION".equals(e.getName())));
    assertTrue("Expected RETAIN env var",
        container.getEnv().stream()
            .anyMatch(e -> "RETAIN".equals(e.getName())));
    assertTrue("Expected BACKUP_TIMEOUT env var",
        container.getEnv().stream()
            .anyMatch(e -> "BACKUP_TIMEOUT".equals(e.getName())));
  }

}
