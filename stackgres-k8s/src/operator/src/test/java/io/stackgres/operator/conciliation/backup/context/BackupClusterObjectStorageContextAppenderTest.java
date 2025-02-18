/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupClusterObjectStorageContextAppenderTest {

  private BackupClusterObjectStorageContextAppender contextAppender;

  private StackGresCluster cluster;

  private StackGresObjectStorage objectStorage;

  @Spy
  private StackGresBackupContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    objectStorage = Fixtures.objectStorage().loadDefault().get();
    contextAppender = new BackupClusterObjectStorageContextAppender(
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
    assertEquals("SGCluster stackgres has no backup configured", ex.getMessage());
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
