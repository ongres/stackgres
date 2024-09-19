/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.external.prometheus.Endpoint;
import io.stackgres.common.crd.external.prometheus.NamespaceSelector;
import io.stackgres.common.crd.external.prometheus.PodMonitor;
import io.stackgres.common.crd.external.prometheus.PodMonitorSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorPrometheusOperatorMonitor;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.PrometheusContext;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class CollectorPodMonitors
    implements ResourceGenerator<StackGresConfigContext> {

  public static final String COLLECTOR_CONFIG_FILE = "otel-collector-config.yaml";
  
  private final LabelFactoryForConfig labelFactory;

  public static String name(StackGresConfig config) {
    return CollectorDeployment.name(config);
  }

  @Inject
  public CollectorPodMonitors(
      LabelFactoryForConfig labelFactory) {
    this.labelFactory = labelFactory;
  }

  /**
   * Create the ConfigMap for Collector.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    if (!Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getDeploy)
        .map(StackGresConfigDeploy::getCollector)
        .orElse(true)
        || context.getObservedClusters().isEmpty()) {
      return Stream.of();
    }

    return context.getPrometheus()
        .stream()
        .map(prometheus -> createPodMonitor(prometheus, context));
  }

  private PodMonitor createPodMonitor(
      PrometheusContext prometheus,
      StackGresConfigContext context) {
    final StackGresConfig config = context.getSource();
    final var monitor = prometheus.getMonitor();
    final Map<String, String> crossNamespaceLabels = labelFactory
        .configCrossNamespaceLabels(config);
    PodMonitor podMonitor = new PodMonitor();
    podMonitor.setMetadata(new ObjectMetaBuilder()
        .withNamespace(monitor
            .map(StackGresConfigCollectorPrometheusOperatorMonitor::getMetadata)
            .map(ObjectMeta::getNamespace)
            .orElse(config.getMetadata().getNamespace()))
        .withName(monitor
            .map(StackGresConfigCollectorPrometheusOperatorMonitor::getMetadata)
            .map(ObjectMeta::getName)
            .orElse(CollectorDeployment.name(config)))
        .withLabels(ImmutableMap.<String, String>builder()
            .putAll(prometheus.getMatchLabels())
            .putAll(crossNamespaceLabels)
            .build())
        .addToLabels(monitor
            .map(StackGresConfigCollectorPrometheusOperatorMonitor::getMetadata)
            .map(ObjectMeta::getLabels)
            .orElse(Map.of()))
        .withAnnotations(monitor
            .map(StackGresConfigCollectorPrometheusOperatorMonitor::getMetadata)
            .map(ObjectMeta::getAnnotations)
            .orElse(Map.of()))
        .withOwnerReferences(monitor
            .map(StackGresConfigCollectorPrometheusOperatorMonitor::getMetadata)
            .map(ObjectMeta::getOwnerReferences)
            .orElse(List.of()))
        .build());
    final Map<String, String> collectorSelectorLabels = labelFactory
        .collectorLabels(config);
    podMonitor.setSpec(monitor
        .map(StackGresConfigCollectorPrometheusOperatorMonitor::getSpec)
        .orElseGet(PodMonitorSpec::new));
    NamespaceSelector namespaceSelector = new NamespaceSelector();
    namespaceSelector.setMatchNames(List.of(config.getMetadata().getNamespace()));
    podMonitor.getSpec().setNamespaceSelector(namespaceSelector);
    podMonitor.getSpec().setSelector(new LabelSelector());
    podMonitor.getSpec().getSelector().setMatchLabels(collectorSelectorLabels);
    if (podMonitor.getSpec().getPodMetricsEndpoints() == null) {
      Endpoint endpoint = new Endpoint();
      endpoint.setPort(CollectorConfigMap.COLLECTOR_DEFAULT_PROMETHEUS_EXPORTER_PORT_NAME);
      endpoint.setPath("/metrics");
      podMonitor.getSpec().setPodMetricsEndpoints(List.of(endpoint));
    }
    return podMonitor;
  }

}
