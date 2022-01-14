/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.prometheus.Endpoint;
import io.stackgres.common.prometheus.NamespaceSelector;
import io.stackgres.common.prometheus.ServiceMonitor;
import io.stackgres.common.prometheus.ServiceMonitorSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V11)
public class PrometheusIntegration implements ResourceGenerator<StackGresClusterContext> {

  public static final String SERVICE = "-pgexp";
  public static final String SERVICE_MONITOR = "-stackgres-postgres-exporter";
  private static final String POSTGRES_EXPORTER_CONTAINER_NAME = StackgresClusterContainers
      .POSTGRES_EXPORTER;
  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  @Inject
  public PrometheusIntegration(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String serviceMonitorName(StackGresClusterContext clusterContext) {
    String namespace = clusterContext.getSource().getMetadata().getNamespace();
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(namespace + "-" + name + SERVICE_MONITOR);
  }

  public static String serviceName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + SERVICE);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final Map<String, String> crossNamespaceLabels = labelFactory
        .clusterCrossNamespaceLabels(cluster);
    final Map<String, String> clusterSelectorLabels = labelFactory.clusterLabels(cluster);
    final String clusterNamespace = cluster.getMetadata().getNamespace();
    final Stream<HasMetadata> resources = Stream.of(
        new ServiceBuilder()
            .withNewMetadata()
            .withNamespace(clusterNamespace)
            .withName(serviceName(context))
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(crossNamespaceLabels)
                .put("container", POSTGRES_EXPORTER_CONTAINER_NAME)
                .build())
            .endMetadata()
            .withSpec(new ServiceSpecBuilder()
                .withSelector(clusterSelectorLabels)
                .withPorts(new ServicePortBuilder()
                    .withName(POSTGRES_EXPORTER_CONTAINER_NAME)
                    .withProtocol("TCP")
                    .withPort(9187)
                    .build())
                .build())
            .build());

    Optional<Stream<HasMetadata>> serviceMonitors = context.getPrometheus()
        .filter(c -> Optional.ofNullable(c.getCreateServiceMonitor()).orElse(false))
        .map(c -> c.getPrometheusInstallations().stream().map(pi -> {
          ServiceMonitor serviceMonitor = new ServiceMonitor();
          serviceMonitor.setMetadata(new ObjectMetaBuilder()
              .withNamespace(pi.getNamespace())
              .withName(serviceMonitorName(context))
              .withLabels(ImmutableMap.<String, String>builder()
                  .putAll(pi.getMatchLabels())
                  .putAll(crossNamespaceLabels)
                  .build())
              .build());

          ServiceMonitorSpec spec = new ServiceMonitorSpec();
          serviceMonitor.setSpec(spec);
          LabelSelector selector = new LabelSelector();
          spec.setSelector(selector);
          NamespaceSelector namespaceSelector = new NamespaceSelector();
          namespaceSelector.setMatchNames(ImmutableList.of(clusterNamespace));
          spec.setNamespaceSelector(namespaceSelector);

          selector.setMatchLabels(crossNamespaceLabels);
          Endpoint endpoint = new Endpoint();
          endpoint.setPort(POSTGRES_EXPORTER_CONTAINER_NAME);
          spec.setEndpoints(Collections.singletonList(endpoint));
          return serviceMonitor;
        }));

    return serviceMonitors
        .map(hasMetadataStream -> Stream.concat(resources, hasMetadataStream))
        .orElse(resources);

  }
}
