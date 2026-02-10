/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ConfigLabelFactory;
import io.stackgres.common.labels.ConfigLabelMapper;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectorServiceTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  @Mock
  private ObservedClusterContext observedClusterContext;

  private CollectorService collectorService;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    collectorService = new CollectorService(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getObservedClusters()).thenReturn(List.of(observedClusterContext));
  }

  @Test
  void generateResource_shouldGenerateOneService() {
    List<HasMetadata> resources = collectorService.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof Service);
  }

  @Test
  void generateResource_whenNoObservedClusters_shouldReturnEmpty() {
    when(context.getObservedClusters()).thenReturn(List.of());

    List<HasMetadata> resources = collectorService.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectNameAndPort() {
    List<HasMetadata> resources = collectorService.generateResource(context).toList();

    Service service = (Service) resources.get(0);
    assertEquals("stackgres-collector", service.getMetadata().getName());

    List<ServicePort> ports = service.getSpec().getPorts();
    assertTrue(ports.stream().anyMatch(port ->
        port.getPort().equals(4317)
            && "oltp-port".equals(port.getName())
            && "TCP".equals(port.getProtocol())));
  }

  @Test
  void generateResource_whenDeployCollectorFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setCollector(false);

    List<HasMetadata> resources = collectorService.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

}
