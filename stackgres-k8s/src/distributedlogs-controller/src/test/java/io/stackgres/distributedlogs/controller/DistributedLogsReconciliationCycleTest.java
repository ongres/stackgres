/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusDatabase;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.distributedlogs.common.DistributedLogsProperty;
import io.stackgres.distributedlogs.common.ImmutableStackGresDistributedLogsContext;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;
import io.stackgres.distributedlogs.configuration.DistributedLogsPropertyContext;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class DistributedLogsReconciliationCycleTest {

  @Mock
  private DistributedLogsPropertyContext propertyContext;

  @Mock
  private DistributedLogsDatabaseReconciliator databaseReconciliator;

  @Mock
  private DistributedLogsConfigReconciliator configReconciliator;

  @Mock
  private CustomResourceScheduler<StackGresDistributedLogs> distributedLogsScheduler;

  @Mock
  private EventController eventController;

  private DistributedLogsControllerReconciliator reconciliator;

  @BeforeEach
  void setUp() {
    reconciliator = DistributedLogsControllerReconciliator.create(p -> {
      p.propertyContext = propertyContext;
      p.databaseReconciliator = databaseReconciliator;
      p.configReconciliator = configReconciliator;
      p.distributedLogsScheduler = distributedLogsScheduler;
      p.eventController = eventController;
    });
  }

  private ImmutableStackGresDistributedLogsContext getDistributedLogsContext() {
    StackGresDistributedLogs distributedLogs = JsonUtil
        .readFromJson("distributedlogs/list.json",
            StackGresDistributedLogsList.class).getItems().get(0);
    return ImmutableStackGresDistributedLogsContext.builder()
        .distributedLogs(distributedLogs)
        .cluster(getStackGresCLusterForDistributedLogs(distributedLogs))
        .addExistingResources(Tuple.tuple(new PodBuilder()
            .withNewMetadata()
            .withName("stackgres-0")
            .endMetadata()
            .withNewStatus()
            .withContainerStatuses(new ContainerStatusBuilder()
                .withName(StackgresClusterContainers.PATRONI)
                .withReady(true)
                .build())
            .endStatus()
            .build(), Optional.empty()))
        .build();
  }

  @Test
  void testDistributedLogsParsing() {
    getDistributedLogsContext();
  }

  @Test
  void testReconciliationWithNullStatus_isSkipped() throws Exception {
    StackGresDistributedLogsContext distributedLogsContext = getDistributedLogsContext();
    distributedLogsContext.getDistributedLogs().setStatus(null);
    reconciliator.reconcile(null, distributedLogsContext);
    verify(databaseReconciliator, times(0)).existsDatabase(any(), any());
    verify(databaseReconciliator, times(0)).createDatabase(any(), any());
    verify(databaseReconciliator, times(0)).updateRetention(any(), any(), any(), any());
    verify(databaseReconciliator, times(0)).reconcileRetention(any(), any(), any(), any());
    verify(distributedLogsScheduler, times(0)).update(any());
  }

  @Test
  void testReconciliationWithNonReadyPatroni_isSkipped() throws Exception {
    when(propertyContext.getString(
        same(DistributedLogsProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME)))
        .thenReturn("stackgres-0");
    StackGresDistributedLogsContext distributedLogsContext = getDistributedLogsContext();
    Pod.class.cast(distributedLogsContext.getExistingResources().get(0).v1)
        .getStatus().getContainerStatuses().get(0).setReady(false);
    reconciliator.reconcile(null, distributedLogsContext);
    verify(databaseReconciliator, times(0)).existsDatabase(any(), any());
    verify(databaseReconciliator, times(0)).createDatabase(any(), any());
    verify(databaseReconciliator, times(0)).updateRetention(any(), any(), any(), any());
    verify(databaseReconciliator, times(0)).reconcileRetention(any(), any(), any(), any());
    verify(distributedLogsScheduler, times(0)).update(any());
  }

  @Test
  void testReconciliationWithEmptyStatus_isPerformed() throws Exception {
    when(propertyContext.getString(
        same(DistributedLogsProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME)))
        .thenReturn("stackgres-0");
    StackGresDistributedLogsContext distributedLogsContext = getDistributedLogsContext();
    reconciliator.reconcile(null, distributedLogsContext);
    verify(databaseReconciliator, times(1)).existsDatabase(any(), any());
    verify(databaseReconciliator, times(1)).createDatabase(any(), any());
    verify(databaseReconciliator, times(2)).updateRetention(any(), any(), any(), any());
    verify(databaseReconciliator, times(2)).reconcileRetention(any(), any(), any(), any());
    verify(distributedLogsScheduler, times(1)).update(any());
  }

  @Test
  void testReconciliationWithDatabaseCreated_isPerformedWithoutDatabaseCreation() throws Exception {
    when(propertyContext.getString(
        same(DistributedLogsProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME)))
        .thenReturn("stackgres-0");
    when(databaseReconciliator.existsDatabase(any(), any()))
        .thenReturn(true);
    StackGresDistributedLogsContext distributedLogsContext = getDistributedLogsContext();
    reconciliator.reconcile(null, distributedLogsContext);
    verify(databaseReconciliator, times(1)).existsDatabase(any(), any());
    verify(databaseReconciliator, times(0)).createDatabase(any(), any());
    verify(databaseReconciliator, times(2)).updateRetention(any(), any(), any(), any());
    verify(databaseReconciliator, times(2)).reconcileRetention(any(), any(), any(), any());
    verify(distributedLogsScheduler, times(1)).update(any());
  }

  @Test
  void testReconciliationWithEmptyDatabaseStatus_isPerformed() throws Exception {
    when(propertyContext.getString(
        same(DistributedLogsProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME)))
        .thenReturn("stackgres-0");
    StackGresDistributedLogsContext distributedLogsContext = getDistributedLogsContext();
    StackGresDistributedLogsStatusDatabase databaseStatus =
        new StackGresDistributedLogsStatusDatabase();
    databaseStatus.setName("stackgres_stackgres");
    distributedLogsContext.getDistributedLogs().getStatus().setDatabases(
        Seq.of(databaseStatus).toList());
    reconciliator.reconcile(null, distributedLogsContext);
    verify(databaseReconciliator, times(1)).existsDatabase(any(), any());
    verify(databaseReconciliator, times(1)).createDatabase(any(), any());
    verify(databaseReconciliator, times(2)).updateRetention(any(), any(), any(), any());
    verify(databaseReconciliator, times(2)).reconcileRetention(any(), any(), any(), any());
    verify(distributedLogsScheduler, times(1)).update(any());
  }

  @Test
  void testReconciliationWithRetentionUpdated_isPerformedWithoutUpdatingRetention() throws Exception {
    when(propertyContext.getString(
        same(DistributedLogsProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME)))
        .thenReturn("stackgres-0");
    StackGresDistributedLogsContext distributedLogsContext = getDistributedLogsContext();
    StackGresDistributedLogsStatusDatabase databaseStatus =
        new StackGresDistributedLogsStatusDatabase();
    databaseStatus.setName("stackgres_stackgres");
    databaseStatus.setRetention("1 minute");
    distributedLogsContext.getDistributedLogs().getStatus().setDatabases(
        Seq.of(databaseStatus).toList());
    reconciliator.reconcile(null, distributedLogsContext);
    verify(databaseReconciliator, times(1)).existsDatabase(any(), any());
    verify(databaseReconciliator, times(1)).createDatabase(any(), any());
    verify(databaseReconciliator, times(0)).updateRetention(any(), any(), any(), any());
    verify(databaseReconciliator, times(2)).reconcileRetention(any(), any(), any(), any());
    verify(distributedLogsScheduler, times(1)).update(any());
  }

  @Test
  void testReconciliationWithoutRetention_isPerformed() throws Exception {
    when(propertyContext.getString(
        same(DistributedLogsProperty.DISTRIBUTEDLOGS_CONTROLLER_POD_NAME)))
        .thenReturn("stackgres-0");
    StackGresDistributedLogsContext distributedLogsContext = getDistributedLogsContext();
    distributedLogsContext.getDistributedLogs()
        .getStatus().getConnectedClusters().get(0).getConfig().setRetention(null);
    reconciliator.reconcile(null, distributedLogsContext);
    verify(databaseReconciliator, times(1)).existsDatabase(any(), any());
    verify(databaseReconciliator, times(1)).createDatabase(any(), any());
    verify(databaseReconciliator, times(2)).updateRetention(any(), any(), any(), any());
    verify(databaseReconciliator, times(0)).reconcileRetention(any(), any(), any(), any());
    verify(distributedLogsScheduler, times(1)).update(any());
  }

  private StackGresCluster getStackGresCLusterForDistributedLogs(
      StackGresDistributedLogs distributedLogs) {
    final StackGresCluster distributedLogsCluster = new StackGresCluster();
    distributedLogsCluster.getMetadata().setNamespace(
        distributedLogs.getMetadata().getNamespace());
    distributedLogsCluster.getMetadata().setName(
        distributedLogs.getMetadata().getName());
    distributedLogsCluster.getMetadata().setUid(
        distributedLogs.getMetadata().getUid());
    return distributedLogsCluster;
  }

}
