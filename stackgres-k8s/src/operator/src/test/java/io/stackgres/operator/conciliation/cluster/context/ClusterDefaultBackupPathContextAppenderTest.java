/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;

import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterDefaultBackupPathContextAppenderTest {

  private ClusterDefaultBackupPathContextAppender contextAppender;

  private StackGresCluster cluster;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  private Instant defaultTimestamp;

  @BeforeEach
  void setUp() throws Exception {
    cluster = Fixtures.cluster().loadDefault().get();
    defaultTimestamp = Instant.now();
    contextAppender = new ClusterDefaultBackupPathContextAppender(
        defaultTimestamp);
  }

  @Test
  void clusterWithoutBackups_shouldSetNothing() {
    cluster.getSpec().getConfigurations().setBackups(null);

    contextAppender.appendContext(cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion());

    assertNull(cluster.getStatus().getBackupPaths());
  }

  @Test
  void clusterWithBackupPathAlreadySet_shouldSetNothing() {
    String backupPath = cluster.getSpec().getConfigurations().getBackups().getFirst().getPath();
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setBackupPaths(List.of(backupPath));

    contextAppender.appendContext(cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion());

    assertEquals(List.of(backupPath), cluster.getStatus().getBackupPaths());
  }

  @Test
  void clusterWithoutBackupPath_shouldSetDefault() {
    cluster.getSpec().getConfigurations().getBackups().getFirst().setPath(null);
    contextAppender.appendContext(cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion());

    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster)
        .getMajorVersion(postgresVersion);

    assertEquals(
        List.of(BackupStorageUtil.getPath(
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName(),
            defaultTimestamp,
            postgresMajorVersion)),
        cluster.getStatus().getBackupPaths());
  }

  @Test
  void clusterWithBackupPath_shouldSetIt() {
    String customBackupPath = "test";
    cluster.getSpec().getConfigurations().getBackups().getFirst().setPath(customBackupPath);

    contextAppender.appendContext(cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion());

    assertEquals(
        List.of(customBackupPath),
        cluster.getStatus().getBackupPaths());
  }

  @Test
  void clusterWithBackupPathSetAndWithoutBackups_shouldChangeNothing() {
    String backupPath = cluster.getSpec().getConfigurations().getBackups().getFirst().getPath();
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setBackupPaths(List.of(backupPath));
    cluster.getSpec().getConfigurations().setBackups(null);

    contextAppender.appendContext(cluster, contextBuilder, cluster.getSpec().getPostgres().getVersion());

    assertEquals(List.of(backupPath), cluster.getStatus().getBackupPaths());
  }

}
