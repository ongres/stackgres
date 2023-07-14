/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.ImmutableStackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.PostgresSocketMount;
import io.stackgres.operator.conciliation.factory.ScriptTemplatesVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.ImmutableClusterContainerContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

class PostgresExporterTest {

  @Inject
  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  private ContainerUserOverrideMounts containerUserOverrideMounts;

  @Inject
  private PostgresSocketMount postgresSocket;

  @Inject
  private ScriptTemplatesVolumeMounts scriptTemplatesVolumeMounts;

  @InjectMocks
  private PostgresExporter postgresExporter;

  @BeforeEach
  public void setupClass() {
    this.postgresExporter = new PostgresExporter(labelFactory, containerUserOverrideMounts,
        postgresSocket, scriptTemplatesVolumeMounts);
  }

  @Test
  void ifDisableMetricsExporterIsNotSpecified_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    assertTrue(postgresExporter.isActivated(context));
  }

  @Test
  void ifDisableMetricsExporterIsFalse_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getSource().getSpec().getPod().setDisableMetricsExporter(false);
    assertTrue(postgresExporter.isActivated(context));
  }

  @Test
  void ifDisableMetricsExporterIsTrue_shouldNotBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getSource().getSpec().getPod().setDisableMetricsExporter(true);
    assertFalse(postgresExporter.isActivated(context));
  }

  private ClusterContainerContext getClusterContainerContext() {
    return ImmutableClusterContainerContext.builder()
        .clusterContext(ImmutableStackGresClusterContext.builder()
            .source(getDefaultCluster())
            .postgresConfig(new StackGresPostgresConfig())
            .profile(new StackGresProfile())
            .build())
        .dataVolumeName("test")
        .build();
  }

  private StackGresCluster getDefaultCluster() {
    return Fixtures.cluster().loadDefault().get();
  }
}
