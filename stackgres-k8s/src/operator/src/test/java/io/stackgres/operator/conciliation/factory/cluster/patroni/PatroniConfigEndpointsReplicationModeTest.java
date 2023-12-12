/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresReplicationMode;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.parameters.PostgresDefaultValues;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniConfigEndpointsReplicationModeTest {

  private static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();
  private final LabelFactoryForCluster<StackGresCluster> labelFactory = new ClusterLabelFactory(
      new ClusterLabelMapper());
  @Mock
  private StackGresClusterContext context;
  private PatroniConfigEndpoints generator;
  private StackGresCluster cluster;
  private StackGresObjectStorage objectStorage;
  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {
    generator = new PatroniConfigEndpoints(JSON_MAPPER, labelFactory);

    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().getAnnotations()
        .put(StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    cluster.getSpec().setDistributedLogs(null);
    cluster.getSpec().getMetadata().getLabels().setServices(null);
    objectStorage = Fixtures.objectStorage().loadDefault().get();
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    postgresConfig.setStatus(new StackGresPostgresConfigStatus());
    setDefaultParameters(postgresConfig);

    lenient().when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    lenient().when(context.getPostgresConfig()).thenReturn(postgresConfig);
  }

  private void setDefaultParameters(StackGresPostgresConfig postgresConfig) {
    final String version = postgresConfig.getSpec().getPostgresVersion();
    postgresConfig.getStatus()
        .setDefaultParameters(PostgresDefaultValues.getDefaultValues(version));
  }

  @Test
  void setDefaultReplicationMode_shouldNotSetPatroniSynchronousMode() {
    when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    when(context.getBackupStorage()).thenCallRealMethod();
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);

    PatroniConfig patroniConfig = generator.getPatroniConfig(context);

    Assertions.assertFalse(patroniConfig.getSynchronousMode());
    Assertions.assertFalse(patroniConfig.getSynchronousModeStrict());
    Assertions.assertNull(patroniConfig.getSynchronousNodeCount());
  }

  @Test
  void setSyncReplicationMode_shouldSetPatroniSynchronousMode() {
    when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    when(context.getBackupStorage()).thenCallRealMethod();
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().setInstances(2);
    cluster.getSpec().getReplication().setMode(StackGresReplicationMode.SYNC.toString());
    cluster.getSpec().getReplication().setSyncInstances(1);

    PatroniConfig patroniConfig = generator.getPatroniConfig(context);

    Assertions.assertTrue(patroniConfig.getSynchronousMode());
    Assertions.assertFalse(patroniConfig.getSynchronousModeStrict());
    Assertions.assertEquals(1, patroniConfig.getSynchronousNodeCount());
  }

  @Test
  void setStrictSyncReplicationMode_shouldSetPatroniSynchronousMode() {
    when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    when(context.getBackupStorage()).thenCallRealMethod();
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().setInstances(2);
    cluster.getSpec().getReplication().setMode(StackGresReplicationMode.STRICT_SYNC.toString());
    cluster.getSpec().getReplication().setSyncInstances(1);

    PatroniConfig patroniConfig = generator.getPatroniConfig(context);

    Assertions.assertTrue(patroniConfig.getSynchronousMode());
    Assertions.assertTrue(patroniConfig.getSynchronousModeStrict());
    Assertions.assertEquals(1, patroniConfig.getSynchronousNodeCount());
  }

  @Test
  void setSyncReplicationModeWith3Instances_shouldSetPatroniSynchronousMode() {
    when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    when(context.getBackupStorage()).thenCallRealMethod();
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().setInstances(3);
    cluster.getSpec().getReplication().setMode(StackGresReplicationMode.SYNC.toString());
    cluster.getSpec().getReplication().setSyncInstances(1);

    PatroniConfig patroniConfig = generator.getPatroniConfig(context);

    Assertions.assertTrue(patroniConfig.getSynchronousMode());
    Assertions.assertFalse(patroniConfig.getSynchronousModeStrict());
    Assertions.assertEquals(1, patroniConfig.getSynchronousNodeCount());
  }

  @Test
  void setStrictSyncReplicationModeWith3Instances_shouldSetPatroniSynchronousMode() {
    when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    when(context.getBackupStorage()).thenCallRealMethod();
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().setInstances(3);
    cluster.getSpec().getReplication().setMode(StackGresReplicationMode.STRICT_SYNC.toString());
    cluster.getSpec().getReplication().setSyncInstances(1);

    PatroniConfig patroniConfig = generator.getPatroniConfig(context);

    Assertions.assertTrue(patroniConfig.getSynchronousMode());
    Assertions.assertTrue(patroniConfig.getSynchronousModeStrict());
    Assertions.assertEquals(1, patroniConfig.getSynchronousNodeCount());
  }

  @Test
  void setSyncAllReplicationMode_shouldSetPatroniSynchronousMode() {
    when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    when(context.getBackupStorage()).thenCallRealMethod();
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().setInstances(2);
    cluster.getSpec().getReplication().setMode(StackGresReplicationMode.SYNC_ALL.toString());
    cluster.getSpec().getReplication().setSyncInstances(1);

    PatroniConfig patroniConfig = generator.getPatroniConfig(context);

    Assertions.assertTrue(patroniConfig.getSynchronousMode());
    Assertions.assertFalse(patroniConfig.getSynchronousModeStrict());
    Assertions.assertEquals(1, patroniConfig.getSynchronousNodeCount());
  }

  @Test
  void setStrictSyncAllReplicationMode_shouldSetPatroniSynchronousMode() {
    when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    when(context.getBackupStorage()).thenCallRealMethod();
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().setInstances(2);
    cluster.getSpec().getReplication().setMode(StackGresReplicationMode.STRICT_SYNC_ALL.toString());
    cluster.getSpec().getReplication().setSyncInstances(1);

    PatroniConfig patroniConfig = generator.getPatroniConfig(context);

    Assertions.assertTrue(patroniConfig.getSynchronousMode());
    Assertions.assertTrue(patroniConfig.getSynchronousModeStrict());
    Assertions.assertEquals(1, patroniConfig.getSynchronousNodeCount());
  }

  @Test
  void setSyncAllReplicationModeWith3Instances_shouldSetPatroniSynchronousMode() {
    when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    when(context.getBackupStorage()).thenCallRealMethod();
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().setInstances(3);
    cluster.getSpec().getReplication().setMode(StackGresReplicationMode.SYNC_ALL.toString());
    cluster.getSpec().getReplication().setSyncInstances(1);

    PatroniConfig patroniConfig = generator.getPatroniConfig(context);

    Assertions.assertTrue(patroniConfig.getSynchronousMode());
    Assertions.assertFalse(patroniConfig.getSynchronousModeStrict());
    Assertions.assertEquals(2, patroniConfig.getSynchronousNodeCount());
  }

  @Test
  void setStrictSyncAllReplicationModeWith3Instances_shouldSetPatroniSynchronousMode() {
    when(context.getObjectStorage()).thenReturn(Optional.of(objectStorage));
    when(context.getBackupStorage()).thenCallRealMethod();
    when(context.getPostgresConfig()).thenReturn(postgresConfig);
    when(context.getSource()).thenReturn(cluster);
    when(context.getCluster()).thenReturn(cluster);
    cluster.getSpec().setInstances(3);
    cluster.getSpec().getReplication().setMode(StackGresReplicationMode.STRICT_SYNC_ALL.toString());
    cluster.getSpec().getReplication().setSyncInstances(1);

    PatroniConfig patroniConfig = generator.getPatroniConfig(context);

    Assertions.assertTrue(patroniConfig.getSynchronousMode());
    Assertions.assertTrue(patroniConfig.getSynchronousModeStrict());
    Assertions.assertEquals(2, patroniConfig.getSynchronousNodeCount());
  }

}
