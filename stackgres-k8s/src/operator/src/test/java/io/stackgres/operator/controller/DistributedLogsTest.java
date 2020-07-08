/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import static org.mockito.Mockito.when;

import io.stackgres.common.DistributedLogsLabelFactory;
import io.stackgres.common.DistributedLogsLabelMapper;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.configuration.OperatorContext;
import io.stackgres.operator.distributedlogs.fluentd.Fluentd;
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
class DistributedLogsTest {

  private StackGresDistributedLogsList distributedLogsList;

  @Mock
  private KubernetesClientFactory clientFactory;

  @Mock
  private Fluentd fluentd;

  @Mock
  private ObjectMapperProvider objectMapperProvider;

  @Mock
  private OperatorContext operatorContext;

  private LabelFactory<StackGresDistributedLogs> labelFactory;

  @Mock
  private CustomResourceScanner<StackGresDistributedLogs> distributedLogsScanner;

  @Mock
  private CustomResourceScanner<StackGresCluster> clusterScanner;

  private DistributedLogsReconciliationCycle reconciliationCycle;

  @BeforeEach
  void setUp() {
    distributedLogsList = JsonUtil.readFromJson("distributedlogs/list.json",
        StackGresDistributedLogsList.class);

    labelFactory = new DistributedLogsLabelFactory(new DistributedLogsLabelMapper());

    reconciliationCycle = new DistributedLogsReconciliationCycle(
        clientFactory, null, fluentd, null, null, null, 
        objectMapperProvider, operatorContext, labelFactory,
        distributedLogsScanner, clusterScanner);
  }

  @Test
  void testDistributedLogsParsing() {
    JsonUtil.readFromJson("distributedlogs/list.json", StackGresDistributedLogsList.class);
  }

  @Test
  void givenSimpleConfigurationWithoutConnectedClusters_itShouldNotFail() {
    when(distributedLogsScanner.getResources()).thenReturn(distributedLogsList.getItems());
    reconciliationCycle.getExistingConfigs();
  }

}
