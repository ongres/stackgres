/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupProcess;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackupStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForShardedBackup;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.shardedcluster.ShardedClusterEnvironmentVariablesFactoryDiscoverer;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupJobTest {

  @Mock
  private LabelFactoryForShardedBackup labelFactory;

  @Mock
  private LabelFactoryForCluster clusterLabelFactory;

  @Mock
  private ResourceFactory<StackGresShardedBackupContext, PodSecurityContext> podSecurityFactory;

  @Mock
  private KubectlUtil kubectl;

  @Mock
  private ShardedClusterEnvironmentVariablesFactoryDiscoverer clusterEnvVarFactoryDiscoverer;

  @Mock
  private ShardedBackupScriptTemplatesVolumeMounts backupScriptTemplatesVolumeMounts;

  @Mock
  private ShardedBackupTemplatesVolumeFactory backupTemplatesVolumeFactory;

  @Mock
  private StackGresShardedBackupContext context;

  private ShardedBackupJob shardedBackupJob;

  private StackGresShardedBackup backup;

  private StackGresShardedCluster shardedCluster;

  private StackGresCluster coordinatorCluster;

  @BeforeEach
  void setUp() {
    shardedBackupJob = new ShardedBackupJob(
        labelFactory,
        clusterLabelFactory,
        podSecurityFactory,
        kubectl,
        clusterEnvVarFactoryDiscoverer,
        backupScriptTemplatesVolumeMounts,
        backupTemplatesVolumeFactory);

    backup = Fixtures.shardedBackup().loadDefault().get();
    shardedCluster = Fixtures.shardedCluster().loadDefault().get();
    coordinatorCluster = Fixtures.cluster().loadDefault().get();
  }

  @Test
  void generateResource_whenBackupNotFinishedAndStoragePresent_shouldGenerateJob() {
    // Status is Running, not finished
    backup.getStatus().getProcess().setStatus("Running");

    configureContextForJobCreation();

    List<HasMetadata> resources = shardedBackupJob.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof Job);
  }

  @Test
  void generateResource_whenBackupAlreadyCompleted_shouldReturnEmpty() {
    StackGresShardedBackupStatus status = new StackGresShardedBackupStatus();
    StackGresShardedBackupProcess process = new StackGresShardedBackupProcess();
    process.setStatus("Completed");
    status.setProcess(process);
    backup.setStatus(status);

    when(context.getSource()).thenReturn(backup);

    List<HasMetadata> resources = shardedBackupJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenBackupAlreadyFailed_shouldReturnEmpty() {
    StackGresShardedBackupStatus status = new StackGresShardedBackupStatus();
    StackGresShardedBackupProcess process = new StackGresShardedBackupProcess();
    process.setStatus("Failed");
    status.setProcess(process);
    backup.setStatus(status);

    when(context.getSource()).thenReturn(backup);

    List<HasMetadata> resources = shardedBackupJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenCrossNamespaceCopy_shouldReturnEmpty() {
    backup.getSpec().setSgShardedCluster("other-namespace.my-cluster");

    when(context.getSource()).thenReturn(backup);

    List<HasMetadata> resources = shardedBackupJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenNoObjectStorage_shouldReturnEmpty() {
    // Set status to not finished (Running)
    backup.getStatus().getProcess().setStatus("Running");

    when(context.getSource()).thenReturn(backup);
    when(context.getObjectStorage()).thenReturn(Optional.empty());

    List<HasMetadata> resources = shardedBackupJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenScheduledBackup_shouldReturnEmpty() {
    backup.getStatus().getProcess().setStatus("Running");
    backup.getMetadata().getAnnotations().put("scheduled-sharded-backup", "true");

    when(context.getSource()).thenReturn(backup);

    List<HasMetadata> resources = shardedBackupJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  private void configureContextForJobCreation() {
    lenient().when(context.getSource()).thenReturn(backup);
    lenient().when(context.getShardedCluster()).thenReturn(shardedCluster);
    lenient().when(context.getCoordinatorCluster()).thenReturn(coordinatorCluster);
    lenient().when(context.getObjectStorage())
        .thenReturn(Optional.of(new StackGresObjectStorage()));
    lenient().when(context.getClusterBackupNamespaces()).thenReturn(Set.of());
    lenient().when(labelFactory.backupPodLabels(backup)).thenReturn(Map.of());
    lenient().when(clusterLabelFactory.clusterLabelsWithoutUid(coordinatorCluster))
        .thenReturn(Map.of());
    lenient().when(kubectl.getImageName(shardedCluster)).thenReturn("kubectl:latest");
    lenient().when(clusterEnvVarFactoryDiscoverer.discoverFactories(context))
        .thenReturn(List.of());
    lenient().when(backupTemplatesVolumeFactory.buildVolumes(context))
        .thenReturn(Stream.of());
    lenient().when(backupScriptTemplatesVolumeMounts.getVolumeMounts(context))
        .thenReturn(List.of());
  }

}
