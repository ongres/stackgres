/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ClusterLabelFactory;
import io.stackgres.common.ClusterLabelMapper;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.cluster.factory.Cluster;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.customresource.prometheus.PrometheusConfig;
import io.stackgres.operator.resource.ClusterSidecarFinder;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class ClusterTest {

  private StackGresClusterList clusterList;

  @Mock
  private OperatorPropertyContext operatorContext;

  private LabelFactory<StackGresCluster> labelFactory;

  @Mock
  private CustomResourceScanner<StackGresCluster> clusterScanner;

  @Mock
  private ClusterSidecarFinder sidecarFinder;

  @Mock
  private Cluster cluster;

  @Mock
  private ClusterStatusManager statusManager;

  @Mock
  private EventController eventController;

  @Mock
  private CustomResourceFinder<StackGresProfile> profileFinder;

  @Mock
  private CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  @Mock
  private CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  @Mock
  private CustomResourceScanner<StackGresBackup> backupScanner;

  @Mock
  private CustomResourceScanner<StackGresDbOps> dbOpsScanner;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private CustomResourceScanner<PrometheusConfig> prometheusScanner;

  private ClusterReconciliationCycle reconciliationCycle;

  @BeforeEach
  void setUp() {
    clusterList = JsonUtil.readFromJson("stackgres_cluster/list.json",
        StackGresClusterList.class);

    labelFactory = new ClusterLabelFactory(new ClusterLabelMapper());

    reconciliationCycle = ClusterReconciliationCycle.create(p -> {
      p.clientFactory = () -> null;
      p.operatorContext = operatorContext;
      p.labelFactory = labelFactory;
      p.clusterScanner = clusterScanner;
      p.sidecarFinder = sidecarFinder;
      p.cluster = cluster;
      p.statusManager = statusManager;
      p.eventController = eventController;
      p.operatorContext = operatorContext;
      p.labelFactory = labelFactory;
      p.profileFinder = profileFinder;
      p.postgresConfigFinder = postgresConfigFinder;
      p.backupConfigFinder = backupConfigFinder;
      p.backupScanner = backupScanner;
      p.dbOpsScanner = dbOpsScanner;
      p.secretFinder = secretFinder;
      p.prometheusScanner = prometheusScanner;
    });
  }

  @Test
  void testClusterParsing() {
    JsonUtil.readFromJson("stackgres_cluster/list.json", StackGresClusterList.class);
  }

  @Test
  void givenSimpleConfiguration_itShouldNotFail() {
    when(clusterScanner.getResources()).thenReturn(clusterList.getItems());
    reconciliationCycle.getExistingContextResources().stream().forEach(reconciliationCycle::getContextFromResource);
  }

}
