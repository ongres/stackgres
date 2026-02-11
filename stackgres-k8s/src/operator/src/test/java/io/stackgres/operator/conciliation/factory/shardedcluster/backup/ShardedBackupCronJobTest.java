/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterBackupConfiguration;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.LabelMapperForShardedCluster;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.shardedcluster.ShardedClusterEnvironmentVariablesFactoryDiscoverer;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupCronJobTest {

  @Mock
  private LabelFactoryForShardedCluster labelFactory;

  @Mock
  private LabelFactoryForCluster clusterLabelFactory;

  @Mock
  private ResourceFactory<StackGresShardedClusterContext, PodSecurityContext> podSecurityFactory;

  @Mock
  private KubectlUtil kubectl;

  @Mock
  private ShardedClusterEnvironmentVariablesFactoryDiscoverer clusterEnvVarFactoryDiscoverer;

  @Mock
  private ShardedBackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts;

  @Mock
  private ShardedBackupTemplatesVolumeFactory backupTemplatesVolumeFactory;

  @Mock
  private StackGresShardedClusterContext context;

  @Mock
  private LabelMapperForShardedCluster labelMapper;

  private ShardedBackupCronJob shardedBackupCronJob;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    shardedBackupCronJob = new ShardedBackupCronJob(
        labelFactory,
        clusterLabelFactory,
        podSecurityFactory,
        kubectl,
        clusterEnvVarFactoryDiscoverer,
        backupScriptTemplatesVolumeMounts,
        backupTemplatesVolumeFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getShardedCluster()).thenReturn(cluster);
    lenient().when(labelFactory.scheduledBackupPodLabels(any())).thenReturn(Map.of());
    lenient().when(labelFactory.labelMapper()).thenReturn(labelMapper);
    lenient().when(labelMapper.scheduledShardedBackupKey(any())).thenReturn("backup-key");
    lenient().when(labelMapper.scheduledShardedBackupJobNameKey(any())).thenReturn("job-name-key");
    lenient().when(kubectl.getImageName(any(StackGresShardedCluster.class))).thenReturn("kubectl:latest");
    lenient().when(clusterLabelFactory.clusterLabelsWithoutUid(any(StackGresCluster.class)))
        .thenReturn(Map.of());
    lenient().when(clusterEnvVarFactoryDiscoverer.discoverFactories(any())).thenReturn(List.of());
    lenient().when(backupScriptTemplatesVolumeMounts.getVolumeMounts(any())).thenReturn(List.of());
    lenient().when(backupTemplatesVolumeFactory.buildVolumes(any())).thenReturn(Stream.of());
    lenient().when(podSecurityFactory.createResource(any())).thenReturn(new PodSecurityContext());
    lenient().when(context.getClusterBackupNamespaces()).thenReturn(Set.of());
  }

  @Test
  void generateResource_whenCronScheduleIsSet_shouldGenerateCronJob() {
    setupBackupConfig("0 5 * * *");
    setupCoordinator();

    List<HasMetadata> resources = shardedBackupCronJob.generateResource(context).toList();

    assertFalse(resources.isEmpty());
    assertTrue(resources.stream().anyMatch(CronJob.class::isInstance));
  }

  @Test
  void generateResource_whenNoCronSchedule_shouldGenerateNoResources() {
    cluster.getSpec().setConfigurations(null);

    List<HasMetadata> resources = shardedBackupCronJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenBackupsListIsEmpty_shouldGenerateNoResources() {
    var configurations = new StackGresShardedClusterConfigurations();
    configurations.setBackups(List.of());
    cluster.getSpec().setConfigurations(configurations);

    List<HasMetadata> resources = shardedBackupCronJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenBackupConfigHasNullCronSchedule_shouldGenerateNoResources() {
    var backupConfig = new StackGresShardedClusterBackupConfiguration();
    backupConfig.setCronSchedule(null);
    var configurations = new StackGresShardedClusterConfigurations();
    configurations.setBackups(List.of(backupConfig));
    cluster.getSpec().setConfigurations(configurations);

    List<HasMetadata> resources = shardedBackupCronJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectScheduleFromSpec() {
    String expectedSchedule = "30 2 * * 1";
    setupBackupConfig(expectedSchedule);
    setupCoordinator();

    List<HasMetadata> resources = shardedBackupCronJob.generateResource(context).toList();

    CronJob cronJob = resources.stream()
        .filter(CronJob.class::isInstance)
        .map(CronJob.class::cast)
        .findFirst()
        .orElseThrow(() -> new AssertionError("No CronJob found"));

    assertEquals(expectedSchedule, cronJob.getSpec().getSchedule());
  }

  private void setupBackupConfig(String cronSchedule) {
    var backupConfig = new StackGresShardedClusterBackupConfiguration();
    backupConfig.setCronSchedule(cronSchedule);
    var configurations = new StackGresShardedClusterConfigurations();
    configurations.setBackups(List.of(backupConfig));
    cluster.getSpec().setConfigurations(configurations);
  }

  private void setupCoordinator() {
    var coordinator = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getCoordinator()).thenReturn(coordinator);
  }

}
