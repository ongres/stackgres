/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import static io.stackgres.common.StringUtil.generateRandom;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.Toleration;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsSchedulingBackup;
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
  private LabelFactoryForCluster labelFactory;
  @Mock
  private ResourceFactory<StackGresClusterContext, PodSecurityContext> clusterPodSecurityFactory;
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

  @Test
  public void generateResource_whenTolerationsSet_shouldApplyToJob() {
    Toleration toleration = new Toleration();
    toleration.setKey("dedicated");
    toleration.setOperator("Equal");
    toleration.setValue("backup");
    toleration.setEffect("NoSchedule");

    StackGresClusterPodsSchedulingBackup backupScheduling =
        sgCluster.getSpec().getPods().getScheduling().getBackup();
    backupScheduling.setTolerations(List.of(toleration));

    givenExpectedBackupConfigAndClusterValues();

    Stream<HasMetadata> generatedResources = backupCronJob.generateResource(clusterContext);
    var cronJob = (CronJob) generatedResources.iterator().next();

    var tolerations = cronJob.getSpec().getJobTemplate().getSpec()
        .getTemplate().getSpec().getTolerations();
    assertNotNull("CronJob should have tolerations when set in the scheduling spec", tolerations);
    assertEquals(1, tolerations.size());
    assertEquals("dedicated", tolerations.get(0).getKey());
    assertEquals("Equal", tolerations.get(0).getOperator());
    assertEquals("backup", tolerations.get(0).getValue());
    assertEquals("NoSchedule", tolerations.get(0).getEffect());
  }

  @Test
  public void generateResource_whenCompressionSet_shouldSetEnvVar() {
    String compressionType = "zstd";
    backupConfig =
        new BackupConfiguration(5, "* * * 5 *", compressionType, "/tmp", backupPerformance,
            null, null, null, null, null, null, null);
    sgBackup.getSpec().setSgCluster(sgCluster.getMetadata().getName());

    givenExpectedBackupConfigAndClusterValues();

    Stream<HasMetadata> generatedResources = backupCronJob.generateResource(clusterContext);
    var cronJob = (CronJob) generatedResources.iterator().next();

    List<EnvVar> envVars = cronJob.getSpec().getJobTemplate().getSpec()
        .getTemplate().getSpec().getContainers().get(0).getEnv();
    Optional<EnvVar> compressionEnvVar = envVars.stream()
        .filter(env -> "COMPRESSION".equals(env.getName()))
        .findFirst();
    assertTrue("CronJob should have a COMPRESSION env var", compressionEnvVar.isPresent());
    assertEquals(compressionType, compressionEnvVar.get().getValue());
  }

}
