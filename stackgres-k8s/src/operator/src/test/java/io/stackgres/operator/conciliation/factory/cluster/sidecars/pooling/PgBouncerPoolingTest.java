/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PostgresSocketMounts;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.ImmutableClusterContainerContext;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PgBouncerPoolingTest {

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private UserOverrideMounts userOverrideMounts;

  @Mock
  private PostgresSocketMounts postgresSocketMounts;

  @Mock
  private TemplatesMounts templatesMounts;

  @Mock
  private DefaultPoolingConfigFactory defaultPoolingConfigFactory;

  private PgBouncerPooling pgBouncerPooling;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    pgBouncerPooling = new PgBouncerPooling(
        labelFactory, userOverrideMounts, postgresSocketMounts,
        templatesMounts, defaultPoolingConfigFactory);
    cluster = Fixtures.cluster().loadDefault().get();
  }

  @Test
  void isActivated_whenConnectionPoolingNotDisabled_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    assertTrue(pgBouncerPooling.isActivated(context));
  }

  @Test
  void isActivated_whenDisableConnectionPoolingIsNull_shouldBeActivated() {
    cluster.getSpec().getPods().setDisableConnectionPooling(null);
    ClusterContainerContext context = getClusterContainerContext();
    assertTrue(pgBouncerPooling.isActivated(context));
  }

  @Test
  void isActivated_whenDisableConnectionPoolingIsFalse_shouldBeActivated() {
    cluster.getSpec().getPods().setDisableConnectionPooling(false);
    ClusterContainerContext context = getClusterContainerContext();
    assertTrue(pgBouncerPooling.isActivated(context));
  }

  @Test
  void isActivated_whenDisableConnectionPoolingIsTrue_shouldNotBeActivated() {
    cluster.getSpec().getPods().setDisableConnectionPooling(true);
    ClusterContainerContext context = getClusterContainerContext();
    assertFalse(pgBouncerPooling.isActivated(context));
  }

  @Test
  void getContainer_whenEnvoyDisabled_shouldHaveContainerPorts() {
    cluster.getSpec().getPods().setDisableEnvoy(true);
    ClusterContainerContext context = getClusterContainerContext();

    Container container = pgBouncerPooling.getContainer(context);

    assertEquals(StackGresContainer.PGBOUNCER.getName(), container.getName());

    List<ContainerPort> ports = container.getPorts();
    assertEquals(2, ports.size());
    assertEquals(EnvoyUtil.POSTGRES_PORT_NAME, ports.get(0).getName());
    assertEquals(EnvoyUtil.PG_POOL_PORT, ports.get(0).getContainerPort());
    assertEquals("TCP", ports.get(0).getProtocol());
    assertEquals(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME, ports.get(1).getName());
    assertEquals(EnvoyUtil.PATRONI_PORT, ports.get(1).getContainerPort());
    assertEquals("TCP", ports.get(1).getProtocol());
  }

  @Test
  void getContainer_whenEnvoyNotDisabled_shouldHaveNoPorts() {
    cluster.getSpec().getPods().setDisableEnvoy(false);
    ClusterContainerContext context = getClusterContainerContext();

    Container container = pgBouncerPooling.getContainer(context);

    assertEquals(StackGresContainer.PGBOUNCER.getName(), container.getName());
    assertTrue(container.getPorts().isEmpty());
  }

  @Test
  void getContainer_whenEnvoyDisableIsNull_shouldHaveNoPorts() {
    cluster.getSpec().getPods().setDisableEnvoy(null);
    ClusterContainerContext context = getClusterContainerContext();

    Container container = pgBouncerPooling.getContainer(context);

    assertEquals(StackGresContainer.PGBOUNCER.getName(), container.getName());
    assertTrue(container.getPorts().isEmpty());
  }

  private ClusterContainerContext getClusterContainerContext() {
    return ImmutableClusterContainerContext.builder()
        .clusterContext(StackGresClusterContext.builder()
            .config(getDefaultConfig())
            .source(cluster)
            .postgresConfig(new StackGresPostgresConfig())
            .profile(new StackGresProfile())
            .currentInstances(0)
            .build())
        .dataVolumeName("test")
        .build();
  }

  private StackGresConfig getDefaultConfig() {
    return Fixtures.config().loadDefault().get();
  }

}
