/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.DistributedLogsLabelFactory;
import io.stackgres.common.labels.DistributedLogsLabelMapper;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsClusterTest {

  private final LabelFactoryForDistributedLogs labelFactory =
      new DistributedLogsLabelFactory(new DistributedLogsLabelMapper());

  @Mock
  private StackGresDistributedLogsContext context;

  private DistributedLogsCluster distributedLogsCluster;

  private StackGresDistributedLogs distributedLogs;

  @BeforeEach
  void setUp() {
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    distributedLogsCluster = new DistributedLogsCluster(labelFactory);

    when(context.getSource()).thenReturn(distributedLogs);
    when(context.getCluster()).thenReturn(Optional.empty());
  }

  @Test
  void generateResource_shouldGenerateOneCluster() {
    List<HasMetadata> resources = distributedLogsCluster.generateResource(context)
        .toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof StackGresCluster);
  }

  @Test
  void generateResource_shouldHaveCorrectNameAndNamespace() {
    StackGresCluster cluster = (StackGresCluster) distributedLogsCluster.generateResource(context)
        .toList()
        .get(0);

    assertEquals("distributedlogs", cluster.getMetadata().getName());
    assertEquals("distributed-logs", cluster.getMetadata().getNamespace());
  }

  @Test
  void generateResource_shouldHaveFluentdCustomContainer() {
    StackGresCluster cluster = (StackGresCluster) distributedLogsCluster.generateResource(context)
        .toList()
        .get(0);

    assertNotNull(cluster.getSpec().getPods().getCustomContainers());

    var fluentdContainer = cluster.getSpec().getPods().getCustomContainers().stream()
        .filter(c -> DistributedLogsCluster.NAME.equals(c.getName()))
        .findFirst();

    assertTrue(fluentdContainer.isPresent(),
        "Expected a custom container named '" + DistributedLogsCluster.NAME + "'");
    assertTrue(fluentdContainer.get().getPorts().stream()
            .anyMatch(p -> p.getContainerPort() == DistributedLogsCluster.FORWARD_PORT),
        "Expected fluentd container to have port " + DistributedLogsCluster.FORWARD_PORT);
  }

  @Test
  void generateResource_shouldHavePrimaryServiceEnabled() {
    StackGresCluster cluster = (StackGresCluster) distributedLogsCluster.generateResource(context)
        .toList()
        .get(0);

    assertNotNull(cluster.getSpec().getPostgresServices());
    assertNotNull(cluster.getSpec().getPostgresServices().getPrimary());
    assertTrue(cluster.getSpec().getPostgresServices().getPrimary().getEnabled(),
        "Expected primary postgres service to be enabled");
  }

  @Test
  void generateResource_shouldHavePersistentVolume() {
    StackGresCluster cluster = (StackGresCluster) distributedLogsCluster.generateResource(context)
        .toList()
        .get(0);

    assertNotNull(cluster.getSpec().getPods().getPersistentVolume());
    assertEquals("128Mi", cluster.getSpec().getPods().getPersistentVolume().getSize());
  }

}
