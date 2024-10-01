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
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.ConfigPath;
import io.stackgres.common.crd.external.prometheus.NamespaceSelector;
import io.stackgres.common.crd.external.prometheus.PodMetricsEndpoint;
import io.stackgres.common.crd.external.prometheus.PodMonitor;
import io.stackgres.common.crd.external.prometheus.PodMonitorSpec;
import io.stackgres.common.crd.external.prometheus.SafeTlsConfigBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
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
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder
public class CollectorPodMonitors
    implements ResourceGenerator<StackGresConfigContext> {

  public static final String COLLECTOR_CONFIG_FILE = "otel-collector-config.yaml";
  
  private final LabelFactoryForConfig labelFactory;

  public static String name(StackGresConfig config) {
    return CollectorDeployments.name(config);
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

    return Seq.seq(context.getPrometheus())
        .flatMap(prometheus -> createPodMonitor(prometheus, context));
  }

  private Stream<HasMetadata> createPodMonitor(
      PrometheusContext prometheus,
      StackGresConfigContext context) {
    final StackGresConfig config = context.getSource();
    final var monitor = prometheus.getMonitor();
    final Map<String, String> crossNamespaceLabels = labelFactory
        .configCrossNamespaceLabels(config);
    PodMonitor podMonitor = new PodMonitor();
    final String podMonitorName = monitor
        .map(StackGresConfigCollectorPrometheusOperatorMonitor::getMetadata)
        .map(ObjectMeta::getName)
        .orElse(CollectorDeployments.name(config));
    final String podMonitorNamespace = monitor
        .map(StackGresConfigCollectorPrometheusOperatorMonitor::getMetadata)
        .map(ObjectMeta::getNamespace)
        .orElse(config.getMetadata().getNamespace());
    podMonitor.setMetadata(new ObjectMetaBuilder()
        .withNamespace(podMonitorNamespace)
        .withName(podMonitorName)
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
      PodMetricsEndpoint endpoint = new PodMetricsEndpoint();
      endpoint.setHonorLabels(true);
      endpoint.setHonorTimestamps(true);
      endpoint.setPort(CollectorConfigMaps.COLLECTOR_DEFAULT_PROMETHEUS_EXPORTER_PORT_NAME);
      endpoint.setPath("/metrics");
      if (Optional.of(config.getSpec())
          .map(StackGresConfigSpec::getCollector)
          .map(StackGresConfigCollector::getConfig)
          .map(c -> c.getObject("exporters"))
          .map(c -> c.getObject("prometheus"))
          .map(c -> c.getObject("tls"))
          .isPresent()) {
        endpoint.setScheme("https");
        endpoint.setTlsConfig(
            new SafeTlsConfigBuilder()
            .withServerName(CollectorDeployments.name(config))
            .withNewCa()
            .withNewSecret()
            .withName(podMonitorName)
            .withKey(ConfigPath.CERTIFICATE_PATH.filename())
            .endSecret()
            .endCa()
            .build());
      }
      podMonitor.getSpec().setPodMetricsEndpoints(List.of(endpoint));
    }
    Optional<Secret> podMonitorSecret = context.getCollectorSecret()
        .filter(collectorSecret -> collectorSecret.getData() != null)
        .map(collectorSecret -> new SecretBuilder()
            .withNewMetadata()
            .withNamespace(podMonitorNamespace)
            .withName(podMonitorName)
            .withLabels(crossNamespaceLabels)
            .endMetadata()
            .withData(Seq.seq(collectorSecret.getData())
                .filter(entry -> entry.v1.equals(ConfigPath.CERTIFICATE_PATH.filename()))
                .toMap(Tuple2::v1, Tuple2::v2))
            .build());
    return Seq.<HasMetadata>of(podMonitor).append(podMonitorSecret.stream());
  }

}
