/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.external.prometheus.PodMonitor;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ConfigLabelFactory;
import io.stackgres.common.labels.ConfigLabelMapper;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.common.PrometheusContext;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectorPodMonitorsTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  @Mock
  private ObservedClusterContext observedClusterContext;

  private CollectorPodMonitors collectorPodMonitors;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    collectorPodMonitors = new CollectorPodMonitors(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getConfig()).thenReturn(config);
    lenient().when(context.getObservedClusters()).thenReturn(List.of(observedClusterContext));
    lenient().when(context.getCollectorSecret()).thenReturn(Optional.empty());
  }

  @Test
  void generateResource_whenCollectorDeployedAndPrometheusExists_shouldGeneratePodMonitor() {
    PrometheusContext prometheusContext =
        new PrometheusContext("monitoring", "prometheus", Map.of(), null);
    when(context.getPrometheus()).thenReturn(List.of(prometheusContext));

    List<HasMetadata> resources = collectorPodMonitors.generateResource(context).toList();

    assertFalse(resources.isEmpty());
    assertTrue(resources.stream().anyMatch(r -> r instanceof PodMonitor));
  }

  @Test
  void generateResource_whenNoPrometheus_shouldReturnEmpty() {
    when(context.getPrometheus()).thenReturn(List.of());

    List<HasMetadata> resources = collectorPodMonitors.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenNoObservedClusters_shouldReturnEmpty() {
    when(context.getObservedClusters()).thenReturn(List.of());

    List<HasMetadata> resources = collectorPodMonitors.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenDeployCollectorFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setCollector(false);

    List<HasMetadata> resources = collectorPodMonitors.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

}
