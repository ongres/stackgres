/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.replication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupBuilder;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplication;
import io.stackgres.common.crd.sgcluster.StackGresClusterReplicationInitialization;
import io.stackgres.common.crd.sgcluster.StackGresReplicationInitializationMode;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplicationInitializationNewBackupTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private ReplicationInitializationNewBackup replicationInitializationNewBackup;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    replicationInitializationNewBackup = new ReplicationInitializationNewBackup(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    setupReplication(StackGresReplicationInitializationMode.FROM_NEWLY_CREATED_BACKUP);
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getReplicationInitializationBackup()).thenReturn(Optional.empty());
    lenient().when(context.getReplicationInitializationBackupToCreate())
        .thenReturn(Optional.empty());
    lenient().when(context.getCurrentInstances()).thenReturn(0);
  }

  private void setupReplication(StackGresReplicationInitializationMode mode) {
    StackGresClusterReplication replication = cluster.getSpec().getReplication();
    if (replication == null) {
      replication = new StackGresClusterReplication();
      cluster.getSpec().setReplication(replication);
    }
    StackGresClusterReplicationInitialization initialization =
        new StackGresClusterReplicationInitialization();
    initialization.setMode(mode.mode());
    replication.setInitialization(initialization);
  }

  @Test
  void generateResource_whenModeIsFromNewlyCreatedBackupAndNeedsNewInstances_shouldGenerateBackup() {
    cluster.getSpec().setInstances(3);
    when(context.getCurrentInstances()).thenReturn(1);
    when(context.getReplicationInitializationBackup()).thenReturn(Optional.empty());

    List<HasMetadata> resources = replicationInitializationNewBackup
        .generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof StackGresBackup);
    StackGresBackup backup = (StackGresBackup) resources.get(0);
    assertEquals(cluster.getMetadata().getNamespace(),
        backup.getMetadata().getNamespace());
    assertEquals(cluster.getMetadata().getName(),
        backup.getSpec().getSgCluster());
    assertNotNull(backup.getMetadata().getLabels());
  }

  @Test
  void generateResource_whenModeIsDifferent_shouldReturnEmpty() {
    setupReplication(StackGresReplicationInitializationMode.FROM_EXISTING_BACKUP);
    cluster.getSpec().setInstances(3);

    List<HasMetadata> resources = replicationInitializationNewBackup
        .generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenModeIsFromPrimary_shouldReturnEmpty() {
    setupReplication(StackGresReplicationInitializationMode.FROM_PRIMARY);
    cluster.getSpec().setInstances(3);

    List<HasMetadata> resources = replicationInitializationNewBackup
        .generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenBackupGenerated_shouldHaveManagedLifecycle() {
    cluster.getSpec().setInstances(3);
    when(context.getCurrentInstances()).thenReturn(1);
    when(context.getReplicationInitializationBackup()).thenReturn(Optional.empty());

    List<HasMetadata> resources = replicationInitializationNewBackup
        .generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresBackup backup = (StackGresBackup) resources.get(0);
    assertTrue(backup.getSpec().getManagedLifecycle());
  }

  @Test
  void generateResource_whenExistingBackupMatchesToCreate_shouldGenerateBackup() {
    cluster.getSpec().setInstances(3);

    StackGresBackup existingBackup = new StackGresBackupBuilder()
        .withNewMetadata()
        .withName("existing-backup")
        .withNamespace(cluster.getMetadata().getNamespace())
        .endMetadata()
        .withNewSpec()
        .withSgCluster(cluster.getMetadata().getName())
        .endSpec()
        .build();
    when(context.getReplicationInitializationBackup())
        .thenReturn(Optional.of(existingBackup));
    when(context.getReplicationInitializationBackupToCreate())
        .thenReturn(Optional.of(existingBackup));

    List<HasMetadata> resources = replicationInitializationNewBackup
        .generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof StackGresBackup);
  }

  @Test
  void generateResource_whenEnoughInstances_shouldReturnEmpty() {
    cluster.getSpec().setInstances(2);
    when(context.getCurrentInstances()).thenReturn(3);
    when(context.getReplicationInitializationBackup()).thenReturn(Optional.empty());

    List<HasMetadata> resources = replicationInitializationNewBackup
        .generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }
}
