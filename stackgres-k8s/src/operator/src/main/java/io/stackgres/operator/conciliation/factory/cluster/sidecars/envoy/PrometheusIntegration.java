/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.prometheus.Endpoint;
import io.stackgres.common.prometheus.NamespaceSelector;
import io.stackgres.common.prometheus.ServiceMonitor;
import io.stackgres.common.prometheus.ServiceMonitorSpec;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

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
    final StackGresCluster cluster = context.getSource();
    final Map<String, String> crossNamespaceLabels = labelFactory
        .clusterCrossNamespaceLabels(cluster);
    final Map<String, String> clusterSelectorLabels = labelFactory.patroniClusterLabels(cluster);

    Seq<HasMetadata> resources = Seq.of(
        new ServiceBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(AbstractEnvoy.serviceName(context))
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(crossNamespaceLabels)
                .put("container", AbstractEnvoy.NAME)
                .build())
            .endMetadata()
            .withSpec(new ServiceSpecBuilder()
                .withSelector(clusterSelectorLabels)
                .withPorts(new ServicePortBuilder()
                    .withProtocol("TCP")
                    .withName(AbstractEnvoy.NAME)
                    .withPort(8001)
                    .build())
                .build())
            .build());

    Optional<Stream<HasMetadata>> serviceMonitors = context.getPrometheus()
        .filter(c -> Optional.ofNullable(c.getCreateServiceMonitor()).orElse(false))
        .map(c -> getServiceMonitors(context, crossNamespaceLabels, c));

    return serviceMonitors
        .map(hasMetadataStream -> Stream.concat(resources, hasMetadataStream))
        .orElse(resources);
  }

  @NotNull
  private Stream<HasMetadata> getServiceMonitors(StackGresClusterContext context,
                                                 Map<String, String> labels,
                                                 Prometheus prometheusConfig) {
    return prometheusConfig.getPrometheusInstallations().stream().map(pi -> {
      ServiceMonitor serviceMonitor = new ServiceMonitor();
      serviceMonitor.setMetadata(new ObjectMetaBuilder()
          .withNamespace(pi.getNamespace())
          .withName(AbstractEnvoy.serviceMonitorName(context))
          .withLabels(ImmutableMap.<String, String>builder()
              .putAll(pi.getMatchLabels())
              .putAll(labels)
              .build())
          .build());

      ServiceMonitorSpec spec = new ServiceMonitorSpec();
      serviceMonitor.setSpec(spec);
      LabelSelector selector = new LabelSelector();
      spec.setSelector(selector);
      NamespaceSelector namespaceSelector = new NamespaceSelector();
      namespaceSelector.setAny(true);
      spec.setNamespaceSelector(namespaceSelector);

      selector.setMatchLabels(labels);
      Endpoint endpoint = new Endpoint();
      endpoint.setPort(AbstractEnvoy.NAME);
      endpoint.setPath("/stats/prometheus");
      spec.setEndpoints(Collections.singletonList(endpoint));
      return serviceMonitor;
    });
  }
}
