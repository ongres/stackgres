/*
 * Copyright (C) 2024 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.stackgres.common.crd.postgres.service.StackGresPostgresServiceNodePort;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterServicesTest {

  @Mock
  private LabelFactoryForShardedCluster labelFactory;

  @Mock
  private LabelFactoryForCluster<StackGresCluster> clusterLabelFactory;

  @Mock
  private StackGresShardedClusterContext shardedClusterContext;

  private ShardedClusterServices shardedClusterServices;

  private StackGresShardedCluster defaultShardedCluster;

  @BeforeEach
  void setUp() {
    shardedClusterServices = new ShardedClusterServices(labelFactory, clusterLabelFactory);

    defaultShardedCluster = Fixtures.shardedCluster().loadDefault().get();

    Mockito.when(shardedClusterContext.getSource())
            .thenReturn(defaultShardedCluster);

    Mockito.when(shardedClusterContext.getShardedCluster())
                    .thenReturn(defaultShardedCluster);

    Mockito.lenient().when(labelFactory.genericLabels(Mockito.any(StackGresShardedCluster.class)))
            .thenReturn(ImmutableMap.of());
    Mockito.lenient().when(clusterLabelFactory.clusterLabelsWithoutUid(Mockito.any(StackGresCluster.class)))
            .thenReturn(ImmutableMap.of());

    Mockito.when(shardedClusterContext.getSource())
            .thenReturn(defaultShardedCluster);

    Mockito.when(shardedClusterContext.getShardedCluster())
            .thenReturn(defaultShardedCluster);
  }

  @Test
  void givenCoordinatorAnyServiceHasNodePorts_shouldBeIncluded() {
    enableCoordinatorAnyNodePorts();
    final Stream<HasMetadata> services = shardedClusterServices.generateResource(shardedClusterContext);

    final Service coordinatorAnyService = getCoordinatorAnyService(services);

    final List<Integer> availableNodePorts = coordinatorAnyService.getSpec()
            .getPorts()
            .stream()
            .map(ServicePort::getNodePort)
            .toList();

    Assertions.assertEquals(List.of(30432, 30433), availableNodePorts);
  }

  @Test
  void givenCoordinatorPrimaryServiceHasNodePorts_shouldBeIncluded() {
    enableCoordinatorPrimaryNodePorts();
    final Stream<HasMetadata> services = shardedClusterServices.generateResource(shardedClusterContext);

    final Service coordinatorPrimaryService = getCoordinatorPrimaryService(services);

    final List<Integer> availableNodePorts = coordinatorPrimaryService.getSpec()
            .getPorts()
            .stream()
            .map(ServicePort::getNodePort)
            .toList();

    Assertions.assertEquals(List.of(30432, 30433), availableNodePorts);
  }

  @Test
  void givenShardsAnyPrimariesServiceHasNodePorts_shouldBeIncluded() {
    enableShardsAnyPrimaryNodePorts();
    final Stream<HasMetadata> services = shardedClusterServices.generateResource(shardedClusterContext);

    final Service coordinatorPrimaryService = getShardsAnyPrimaryService(services);

    final List<Integer> availableNodePorts = coordinatorPrimaryService.getSpec()
            .getPorts()
            .stream()
            .map(ServicePort::getNodePort)
            .toList();

    Assertions.assertEquals(List.of(30432, 30433), availableNodePorts);
  }

  private Service getCoordinatorAnyService(Stream<HasMetadata> services) {
    return services
            .filter(Service.class::isInstance)
            .filter(s -> s.getMetadata().getName()
              .equals(StackGresShardedClusterUtil.anyCoordinatorServiceName(defaultShardedCluster)))
            .map(Service.class::cast)
            .findFirst()
            .orElseGet(() -> org.junit.jupiter.api.Assertions.fail("No postgres coordinator any service found"));
  }

  private Service getCoordinatorPrimaryService(Stream<HasMetadata> services) {
    return services
            .filter(Service.class::isInstance)
            .filter(s -> s.getMetadata().getName()
              .equals(StackGresShardedClusterUtil.primaryCoordinatorServiceName(defaultShardedCluster)))
            .map(Service.class::cast)
            .findFirst()
            .orElseGet(() -> org.junit.jupiter.api.Assertions.fail("No postgres coordinator primary service found"));
  }

  private Service getShardsAnyPrimaryService(Stream<HasMetadata> services) {
    return services
            .filter(Service.class::isInstance)
            .filter(s -> s.getMetadata().getName()
              .equals(StackGresShardedClusterUtil.primariesShardsServiceName(defaultShardedCluster)))
            .map(Service.class::cast)
            .findFirst()
            .orElseGet(() -> org.junit.jupiter.api.Assertions.fail("No postgres shards any primary service found"));
  }

  private void enableCoordinatorAnyNodePorts() {
    final StackGresPostgresService coordinatorAny = defaultShardedCluster
            .getSpec()
            .getPostgresServices()
            .getCoordinator()
            .getAny();

    final StackGresPostgresServiceNodePort nodePorts = new StackGresPostgresServiceNodePort();
    nodePorts.setPgport(30432);
    nodePorts.setReplicationport(30433);

    coordinatorAny.setNodePorts(nodePorts);
  }

  private void enableCoordinatorPrimaryNodePorts() {
    final StackGresPostgresService coordinatorPrimary = defaultShardedCluster
            .getSpec()
            .getPostgresServices()
            .getCoordinator()
            .getPrimary();

    final StackGresPostgresServiceNodePort nodePorts = new StackGresPostgresServiceNodePort();
    nodePorts.setPgport(30432);
    nodePorts.setReplicationport(30433);

    coordinatorPrimary.setNodePorts(nodePorts);
  }

  private void enableShardsAnyPrimaryNodePorts() {
    final StackGresPostgresService shardsAnyPrimary = defaultShardedCluster
            .getSpec()
            .getPostgresServices()
            .getShards()
            .getPrimaries();

    final StackGresPostgresServiceNodePort nodePorts = new StackGresPostgresServiceNodePort();
    nodePorts.setPgport(30432);
    nodePorts.setReplicationport(30433);

    shardsAnyPrimary.setNodePorts(nodePorts);
  }
}
