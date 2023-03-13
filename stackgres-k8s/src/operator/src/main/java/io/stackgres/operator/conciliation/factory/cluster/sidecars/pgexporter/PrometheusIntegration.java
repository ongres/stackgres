/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter;

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
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.prometheus.Endpoint;
import io.stackgres.common.prometheus.NamespaceSelector;
import io.stackgres.common.prometheus.PodMonitor;
import io.stackgres.common.prometheus.PodMonitorSpec;
import io.stackgres.common.prometheus.PrometheusInstallation;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V_1_4)
public class PrometheusIntegration implements ResourceGenerator<StackGresClusterContext> {

  public static final String SERVICE = "-pgexp";
  public static final String POD_MONITOR = "-stackgres-postgres-exporter";
  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public PrometheusIntegration(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String podMonitorName(StackGresClusterContext clusterContext) {
    String namespace = clusterContext.getSource().getMetadata().getNamespace();
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(namespace + "-" + name + POD_MONITOR);
  }

  public static String serviceName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + SERVICE);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    Optional<Stream<HasMetadata>> podMonitors = context.getPrometheus()
        .filter(c -> Optional.ofNullable(c.getCreatePodMonitor()).orElse(false))
        .map(c -> c.getPrometheusInstallations().stream()
            .map(prometheusInstallation -> getPodMonitor(
                context, prometheusInstallation)));

    return podMonitors.stream().flatMap(Function.identity());
  }

  private HasMetadata getPodMonitor(StackGresClusterContext context,
      PrometheusInstallation prometheusInstallation) {
    final StackGresCluster cluster = context.getSource();
    final String clusterNamespace = cluster.getMetadata().getNamespace();
    final Map<String, String> clusterSelectorLabels = labelFactory
        .patroniClusterLabels(cluster);
    final Map<String, String> crossNamespaceLabels = labelFactory
        .clusterCrossNamespaceLabels(cluster);
    PodMonitor podMonitor = new PodMonitor();
    podMonitor.setMetadata(new ObjectMetaBuilder()
        .withNamespace(prometheusInstallation.getNamespace())
        .withName(podMonitorName(context))
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
    endpoint.setPort(PostgresExporter.POSTGRES_EXPORTER_PORT_NAME);
    spec.setPodMetricsEndpoints(Collections.singletonList(endpoint));
    return podMonitor;
  }
}
