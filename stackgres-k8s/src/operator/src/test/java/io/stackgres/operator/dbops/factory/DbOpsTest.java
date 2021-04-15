/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.dbops.factory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.StackGresUserClusterContext;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsTest {

  private static final String EMPTY_STRING = "";

  @Mock
  private DbOpsBenchmark benchmark;

  @Mock
  private DbOpsVacuumJob vacuumJob;

  @Mock
  private DbOpsRepackJob repackJob;

  @Mock
  private DbOpsMajorVersionUpgradeJob majorVersionUpgradeJob;

  @Mock
  private DbOpsRestartJob restartJob;

  @Mock
  private DbOpsMinorVersionUpgradeJob minorVersionUpgradeJob;

  @Mock
  private DbOpsSecurityUpgradeJob securityUpgradeJob;

  @Mock
  private DbOpsRole role;

  @Mock
  private StackGresUserClusterContext context;

  private DbOps dbOps;

  @BeforeEach
  void setUp() {
    dbOps = new DbOps(benchmark, vacuumJob, repackJob, majorVersionUpgradeJob, restartJob,
        minorVersionUpgradeJob, securityUpgradeJob, role);
    StackGresCluster clusterResource = getClusterResource();
    when(context.getCluster()).thenReturn(clusterResource);
    when(context.getOperatorContext()).thenReturn(new OperatorPropertyContext());
    when(context.getSidecars()).thenReturn(ImmutableList.of());
    when(context.getBackups()).thenReturn(ImmutableList.of());
    when(context.getInternalScripts()).thenReturn(ImmutableList.of());
    when(context.getExistingResources()).thenReturn(ImmutableList.of());
    when(context.getRequiredResources()).thenReturn(ImmutableList.of());
    when(context.getDbOpsKey()).thenReturn(EMPTY_STRING);
    when(context.getClusterNamespace()).thenReturn(EMPTY_STRING);
    when(context.getClusterKey()).thenReturn(EMPTY_STRING);
    when(context.getClusterName()).thenReturn(EMPTY_STRING);
    when(context.getScheduledBackupKey()).thenReturn(EMPTY_STRING);
    when(context.getBackupKey()).thenReturn(EMPTY_STRING);
    when(context.getDbOpsKey()).thenReturn(EMPTY_STRING);
    when(context.getLabels()).thenReturn(ImmutableMap.of());
    when(context.getOwnerReferences()).thenReturn(ImmutableList.of());
  }

  @Test
  void noRunAt_shouldCreateResources() {
    StackGresDbOps dbOpsResource = getDbOpsResource();
    dbOpsResource.getSpec().setRunAt(null);
    when(context.getDbOps()).thenReturn(ImmutableList.of(dbOpsResource));
    Assertions.assertEquals(0, dbOps.streamResources(context).count());
    verify(benchmark, times(0)).streamResources(any());
    verify(vacuumJob, times(1)).streamResources(any());
    verify(repackJob, times(0)).streamResources(any());
    verify(majorVersionUpgradeJob, times(0)).streamResources(any());
    verify(restartJob, times(0)).streamResources(any());
    verify(minorVersionUpgradeJob, times(0)).streamResources(any());
    verify(securityUpgradeJob, times(0)).streamResources(any());
    verify(role, times(1)).streamResources(any());
  }

  @Test
  void pastRunAt_shouldCreateResources() {
    StackGresDbOps dbOpsResource = getDbOpsResource();
    dbOpsResource.getSpec().setRunAt(Instant.now().minus(1, ChronoUnit.DAYS).toString());
    when(context.getDbOps()).thenReturn(ImmutableList.of(dbOpsResource));
    Assertions.assertEquals(0, dbOps.streamResources(context).count());
    verify(benchmark, times(0)).streamResources(any());
    verify(vacuumJob, times(1)).streamResources(any());
    verify(repackJob, times(0)).streamResources(any());
    verify(majorVersionUpgradeJob, times(0)).streamResources(any());
    verify(restartJob, times(0)).streamResources(any());
    verify(minorVersionUpgradeJob, times(0)).streamResources(any());
    verify(securityUpgradeJob, times(0)).streamResources(any());
    verify(role, times(1)).streamResources(any());
  }

  @Test
  void futureRunAt_shouldNotCreateResources() {
    StackGresDbOps dbOpsResource = getDbOpsResource();
    dbOpsResource.getSpec().setRunAt(Instant.now().plus(1, ChronoUnit.DAYS).toString());
    Mockito.when(context.getDbOps()).thenReturn(ImmutableList.of(dbOpsResource));
    Assertions.assertEquals(0, dbOps.streamResources(context).count());
    verify(benchmark, times(0)).streamResources(any());
    verify(vacuumJob, times(0)).streamResources(any());
    verify(repackJob, times(0)).streamResources(any());
    verify(majorVersionUpgradeJob, times(0)).streamResources(any());
    verify(restartJob, times(0)).streamResources(any());
    verify(minorVersionUpgradeJob, times(0)).streamResources(any());
    verify(securityUpgradeJob, times(0)).streamResources(any());
    verify(role, times(0)).streamResources(any());
  }

  private StackGresCluster getClusterResource() {
    return JsonUtil.readFromJson(
        "stackgres_cluster/default.json", StackGresCluster.class);
  }

  private StackGresDbOps getDbOpsResource() {
    return JsonUtil.readFromJson(
        "dbops/vacuum.json", StackGresDbOps.class);
  }

}