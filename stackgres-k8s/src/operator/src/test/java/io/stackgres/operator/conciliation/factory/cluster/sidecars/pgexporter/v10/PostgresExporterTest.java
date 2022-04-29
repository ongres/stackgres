/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter.v10;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.cluster.ImmutableStackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.ImmutableStackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter.v_1_0.PostgresExporter;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

class PostgresExporterTest {

  @InjectMocks
  private PostgresExporter postgresExporter;

  @BeforeEach
  public void setupClass() {
    this.postgresExporter = new PostgresExporter();
  }

  @Test
  void ifDisableMetricsExporterIsNotSpecified_shouldBeActivated() {
    StackGresClusterContainerContext context = getStackGresClusterContainerContext();
    assertTrue(postgresExporter.isActivated(context));
  }

  @Test
  void ifDisableMetricsExporterIsFalse_shouldBeActivated() {
    StackGresClusterContainerContext context = getStackGresClusterContainerContext();
    context.getClusterContext().getSource().getSpec().getPod().setDisableMetricsExporter(false);
    assertTrue(postgresExporter.isActivated(context));
  }

  @Test
  void ifDisableMetricsExporterIsTrue_shouldNotBeActivated() {
    StackGresClusterContainerContext context = getStackGresClusterContainerContext();
    context.getClusterContext().getSource().getSpec().getPod().setDisableMetricsExporter(true);
    assertFalse(postgresExporter.isActivated(context));
  }

  private ImmutableStackGresClusterContainerContext getStackGresClusterContainerContext() {
    return ImmutableStackGresClusterContainerContext.builder()
        .clusterContext(ImmutableStackGresClusterContext.builder()
            .source(getDefaultCluster())
            .postgresConfig(new StackGresPostgresConfig())
            .stackGresProfile(new StackGresProfile())
            .build())
        .dataVolumeName("test")
        .build();
  }

  private StackGresCluster getDefaultCluster() {
    return JsonUtil.readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
  }
}
