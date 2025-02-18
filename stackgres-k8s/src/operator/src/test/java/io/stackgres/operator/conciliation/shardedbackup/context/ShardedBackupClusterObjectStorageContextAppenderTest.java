/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurationsBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupClusterObjectStorageContextAppenderTest {

  private ShardedBackupClusterObjectStorageContextAppender contextAppender;

  private StackGresShardedCluster cluster;

  private StackGresObjectStorage objectStorage;

  @Spy
  private StackGresShardedBackupContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getSpec().setConfigurations(
        new  StackGresShardedClusterConfigurationsBuilder()
        .addNewBackup()
        .withSgObjectStorage("objstorage")
        .endBackup()
        .build());
    objectStorage = Fixtures.objectStorage().loadDefault().get();
    contextAppender = new ShardedBackupClusterObjectStorageContextAppender(
        objectStorageFinder);
  }

  @Test
  void givenClusterWithObjectStorage_shouldPass() {
    when(objectStorageFinder.findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(objectStorage));
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).objectStorage(Optional.of(objectStorage));
  }

  @Test
  void givenClusterWithoutBackups_shouldFail() {
    cluster.getSpec().getConfigurations().setBackups(List.of());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("SGShardedCluster stackgres has no backup configured", ex.getMessage());
  }

  @Test
  void givenClusterWithoutObjectStorage_shouldFail() {
    when(objectStorageFinder.findByNameAndNamespace(
        cluster.getSpec().getConfigurations().getBackups().get(0).getSgObjectStorage(),
        cluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("SGObjectStorage objstorage was not found", ex.getMessage());
  }

}
