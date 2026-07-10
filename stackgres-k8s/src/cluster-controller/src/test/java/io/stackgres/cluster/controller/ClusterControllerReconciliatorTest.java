/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.resource.CustomResourceWriter;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterControllerReconciliatorTest {

  private static final String POD_NAME = "test-0";

  @Mock
  private CustomResourceWriter<StackGresCluster> clusterWriter;
  @Mock
  private ClusterControllerPostgresBootstrapReconciliator postgresBootstrapReconciliator;
  @Mock
  private ClusterExtensionReconciliator extensionReconciliator;
  @Mock
  private PgBouncerReconciliator pgbouncerReconciliator;
  @Mock
  private ClusterPersistentVolumeSizeReconciliator pvcSizeReconciliator;
  @Mock
  private IoLimitsReconciliator ioLimitsReconciliator;
  @Mock
  private ClusterDataCoherenceReconciliator dataCoherenceReconciliator;
  @Mock
  private ClusterWalRelocationReconciliator walRelocationReconciliator;
  @Mock
  private PatroniReconciliator patroniReconciliator;
  @Mock
  private ManagedSqlReconciliator managedSqlReconciliator;
  @Mock
  private SslReconciliator sslReconciliator;
  @Mock
  private PatroniStandbyReconciliator patroniStandbyReconciliator;
  @Mock
  private PatroniConfigReconciliator patroniConfigReconciliator;
  @Mock
  private PatroniMajorVersionUpgradeReconciliator patroniMajorVersionUpgradeReconciliator;
  @Mock
  private PatroniBackupFailoverRestartReconciliator patroniBackupFailoverRestartReconciliator;
  @Mock
  private PatroniOperationReconciliator patroniOperationReconciliator;
  @Mock
  private ClusterControllerPropertyContext propertyContext;
  @Mock
  private StackGresClusterContext context;
  @Mock
  private KubernetesClient client;

  private ClusterControllerReconciliator reconciliator;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() throws Exception {
    when(propertyContext.getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME))
        .thenReturn(POD_NAME);
    when(propertyContext.get(ClusterControllerProperty.CLUSTER_CONTROLLER_NODE_NAME))
        .thenReturn(java.util.Optional.of("test-node"));
    var parameters = new ClusterControllerReconciliator.Parameters();
    parameters.clusterWriter = clusterWriter;
    parameters.postgresBootstrapReconciliator = postgresBootstrapReconciliator;
    parameters.extensionReconciliator = extensionReconciliator;
    parameters.pgbouncerReconciliator = pgbouncerReconciliator;
    parameters.clusterPersistentVolumeSizeReconciliator = pvcSizeReconciliator;
    parameters.ioLimitsReconciliator = ioLimitsReconciliator;
    parameters.dataCoherenceReconciliator = dataCoherenceReconciliator;
    parameters.walRelocationReconciliator = walRelocationReconciliator;
    parameters.patroniReconciliator = patroniReconciliator;
    parameters.managedSqlReconciliator = managedSqlReconciliator;
    parameters.sslReconciliator = sslReconciliator;
    parameters.patroniStandbyReconciliator = patroniStandbyReconciliator;
    parameters.patroniConfigReconciliator = patroniConfigReconciliator;
    parameters.patroniMajorVersionUpgradeReconciliator = patroniMajorVersionUpgradeReconciliator;
    parameters.patroniBackupFailoverRestartReconciliator =
        patroniBackupFailoverRestartReconciliator;
    parameters.patroniOperationReconciliator = patroniOperationReconciliator;
    parameters.propertyContext = propertyContext;
    parameters.objectMapper = new ObjectMapper();
    reconciliator = new ClusterControllerReconciliator(parameters);

    cluster = new StackGresCluster();
    cluster.setSpec(new StackGresClusterSpec());
    cluster.getMetadata().setName("test");
    cluster.getMetadata().setNamespace("test-namespace");
    when(context.getCluster()).thenReturn(cluster);
    when(clusterWriter.update(any(), any())).thenReturn(cluster);

    lenient().when(postgresBootstrapReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(false));
    lenient().when(extensionReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(false));
    lenient().when(pgbouncerReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
    lenient().when(pvcSizeReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
    lenient().when(ioLimitsReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
    lenient().when(patroniReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(false));
    lenient().when(managedSqlReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(false));
    lenient().when(sslReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
    lenient().when(patroniStandbyReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
    lenient().when(patroniConfigReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
    lenient().when(patroniMajorVersionUpgradeReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
    lenient().when(patroniBackupFailoverRestartReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
    lenient().when(patroniOperationReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>());
  }

  @Test
  void whenDataCoherenceAndWalRelocationAllow_shouldReconcilePatroni() throws Exception {
    when(dataCoherenceReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(true));
    when(walRelocationReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(
            new ClusterWalRelocationReconciliator.WalRelocationResult(false, true)));

    reconciliator.reconcile(client, context);

    verify(patroniReconciliator).reconcile(any(), any());
  }

  @Test
  void whenDataCoherenceBlocks_shouldSkipWalRelocationAndPatroni() throws Exception {
    when(dataCoherenceReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(false));

    reconciliator.reconcile(client, context);

    verify(walRelocationReconciliator, never()).reconcile(any(), any());
    verify(patroniReconciliator, never()).reconcile(any(), any());
  }

  @Test
  void whenWalRelocationBlocks_shouldSkipPatroni() throws Exception {
    when(dataCoherenceReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(true));
    when(walRelocationReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(
            new ClusterWalRelocationReconciliator.WalRelocationResult(false, false)));

    reconciliator.reconcile(client, context);

    verify(patroniReconciliator, never()).reconcile(any(), any());
  }

  @Test
  void whenWalRelocationUpdatesTheCluster_shouldWriteTheClusterStatus() throws Exception {
    when(dataCoherenceReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(true));
    when(walRelocationReconciliator.reconcile(any(), any()))
        .thenReturn(new ReconciliationResult<>(
            new ClusterWalRelocationReconciliator.WalRelocationResult(true, true)));

    reconciliator.reconcile(client, context);

    verify(clusterWriter).update(any(), any());
  }

}
