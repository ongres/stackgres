/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.UserOverrideMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.ImmutableClusterContainerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvoyTest {

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private UserOverrideMounts userOverrideMounts;

  private final YamlMapperProvider yamlMapperProvider = new YamlMapperProvider();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private Envoy envoy;

  @BeforeEach
  void setUp() {
    envoy = new Envoy(yamlMapperProvider, objectMapper, labelFactory, userOverrideMounts);
  }

  @Test
  void isActivated_whenEnvoyNotDisabled_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    // disableEnvoy is not set (null defaults to false) => activated
    Assertions.assertTrue(envoy.isActivated(context));
  }

  @Test
  void isActivated_whenEnvoyExplicitlyNotDisabled_shouldBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods().setDisableEnvoy(false);

    Assertions.assertTrue(envoy.isActivated(context));
  }

  @Test
  void isActivated_whenEnvoyDisabled_shouldNotBeActivated() {
    ClusterContainerContext context = getClusterContainerContext();
    context.getClusterContext().getCluster().getSpec().getPods().setDisableEnvoy(true);

    Assertions.assertFalse(envoy.isActivated(context));
  }

  @Test
  void getContainer_shouldHaveCorrectPorts() {
    ClusterContainerContext context = getClusterContainerContext();

    Container container = envoy.getContainer(context);

    Assertions.assertFalse(container.getPorts().isEmpty(),
        "Container should have ports defined");

    Assertions.assertTrue(
        container.getPorts().stream()
            .map(ContainerPort::getName)
            .anyMatch(name -> name.equals(EnvoyUtil.POSTGRES_PORT_NAME)),
        "Should contain postgres port");
    Assertions.assertTrue(
        container.getPorts().stream()
            .map(ContainerPort::getName)
            .anyMatch(name -> name.equals(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)),
        "Should contain replication port");
    Assertions.assertTrue(
        container.getPorts().stream()
            .map(ContainerPort::getName)
            .anyMatch(name -> name.equals(EnvoyUtil.ENVOY_PORT_NAME)),
        "Should contain envoy admin port");
    Assertions.assertTrue(
        container.getPorts().stream()
            .map(ContainerPort::getName)
            .anyMatch(name -> name.equals(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)),
        "Should contain patroni rest api port");

    Assertions.assertTrue(
        container.getPorts().stream()
            .anyMatch(port -> port.getContainerPort().equals(EnvoyUtil.PG_ENTRY_PORT)),
        "Should have PG entry port " + EnvoyUtil.PG_ENTRY_PORT);
    Assertions.assertTrue(
        container.getPorts().stream()
            .anyMatch(port -> port.getContainerPort().equals(EnvoyUtil.PG_REPL_ENTRY_PORT)),
        "Should have PG replication entry port " + EnvoyUtil.PG_REPL_ENTRY_PORT);
    Assertions.assertTrue(
        container.getPorts().stream()
            .anyMatch(port -> port.getContainerPort().equals(EnvoyUtil.ENVOY_PORT)),
        "Should have envoy port " + EnvoyUtil.ENVOY_PORT);
    Assertions.assertTrue(
        container.getPorts().stream()
            .anyMatch(port -> port.getContainerPort().equals(EnvoyUtil.PATRONI_ENTRY_PORT)),
        "Should have patroni entry port " + EnvoyUtil.PATRONI_ENTRY_PORT);
  }

  private ClusterContainerContext getClusterContainerContext() {
    return ImmutableClusterContainerContext.builder()
        .clusterContext(StackGresClusterContext.builder()
            .config(getDefaultConfig())
            .source(getDefaultCluster())
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

  private StackGresCluster getDefaultCluster() {
    return Fixtures.cluster().loadDefault().get();
  }

}
