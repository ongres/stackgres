/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.prometheus.Endpoint;
import io.stackgres.common.prometheus.NamespaceSelector;
import io.stackgres.common.prometheus.PodMonitor;
import io.stackgres.common.prometheus.PodMonitorSpec;
import io.stackgres.common.prometheus.PrometheusInstallation;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class PrometheusIntegration implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public PrometheusIntegration(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    Optional<Stream<HasMetadata>> podMonitors = context.getPrometheus()
        .filter(c -> Optional.ofNullable(c.getCreatePodMonitor()).orElse(false))
        .map(c -> getPodMonitors(context, c));

    return podMonitors.stream().flatMap(Function.identity());
  }

  @NotNull
  private Stream<HasMetadata> getPodMonitors(
      StackGresClusterContext context,
      Prometheus prometheusConfig) {
    return prometheusConfig.getPrometheusInstallations().stream()
        .map(prometheusInstallation -> getPodMonitor(context, prometheusInstallation));
  }

  private HasMetadata getPodMonitor(StackGresClusterContext context,
      PrometheusInstallation prometheusInstallation) {
    final StackGresCluster cluster = context.getSource();
    final String clusterNamespace = cluster.getMetadata().getNamespace();
    final Map<String, String> crossNamespaceLabels = labelFactory
        .clusterCrossNamespaceLabels(cluster);
    final Map<String, String> clusterSelectorLabels = labelFactory
        .clusterLabels(cluster);
    PodMonitor podMonitor = new PodMonitor();
    podMonitor.setMetadata(new ObjectMetaBuilder()
        .withNamespace(prometheusInstallation.getNamespace())
        .withName(Envoy.podMonitorName(context))
        .withLabels(ImmutableMap.<String, String>builder()
            .putAll(prometheusInstallation.getMatchLabels())
            .putAll(crossNamespaceLabels)
            .build())
        .build());

    PodMonitorSpec spec = new PodMonitorSpec();
    podMonitor.setSpec(spec);
    LabelSelector selector = new LabelSelector();
    spec.setSelector(selector);
    NamespaceSelector namespaceSelector = new NamespaceSelector();
    namespaceSelector.setMatchNames(List.of(clusterNamespace));
    spec.setNamespaceSelector(namespaceSelector);

    selector.setMatchLabels(clusterSelectorLabels);
    Endpoint endpoint = new Endpoint();
    endpoint.setPort(String.valueOf(EnvoyUtil.ENVOY_PORT));
    endpoint.setPath("/stats/prometheus");
    spec.setPodMetricsEndpoints(Collections.singletonList(endpoint));
    return podMonitor;
  }
}
