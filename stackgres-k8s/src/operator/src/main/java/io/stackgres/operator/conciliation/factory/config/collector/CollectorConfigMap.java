/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ConfigPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.JsonArray;
import io.stackgres.common.crd.JsonObject;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterObservability;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesConfigMap;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter.PostgresExporter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class CollectorConfigMap
    extends AbstractTemplatesConfigMap
    implements ResourceGenerator<StackGresConfigContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger("io.stackgres.collector");

  public static final String COLLECTOR_DEFAULT_PROMETHEUS_EXPORTER_PORT_NAME = "prom-http";
  public static final Integer COLLECTOR_DEFAULT_PROMETHEUS_EXPORTER_PORT = 9464;

  public static final Integer OTEL_HEALTH_CHECK_PORT = 13133;
  public static final String OTEL_HEALTH_CHECK_PATH = "/";

  private final LabelFactoryForConfig labelFactory;
  private final YAMLMapper yamlMapper;

  public static String name(StackGresConfig config) {
    return CollectorDeployment.name(config);
  }

  @Inject
  public CollectorConfigMap(
      LabelFactoryForConfig labelFactory,
      YamlMapperProvider yamlMapperProvider) {
    this.labelFactory = labelFactory;
    this.yamlMapper = yamlMapperProvider.get();
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

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);

    JsonObject otelConfig = Unchecked.supplier(() -> yamlMapper.treeToValue(
        yamlMapper.valueToTree(config.getSpec().getCollector().getConfig()),
        JsonObject.class)).get();

    var healthCheck = otelConfig
        .getObjectOrPut("extensions")
        .getObjectOrPut("health_check");
    healthCheck.put("endpoint", "0.0.0.0:" + OTEL_HEALTH_CHECK_PORT);
    healthCheck.put("path", OTEL_HEALTH_CHECK_PATH);
    JsonArray extensions = otelConfig
        .getObjectOrPut("service")
        .getArrayOrPut("extensions");
    otelConfig
        .getObjectOrPut("service")
        .put("extensions", Seq.seq(extensions)
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter("health_check"::equals)
            .append("health_check")
            .toList());

    final JsonObject receivers = otelConfig
        .getObjectOrPut("receivers");
    context.getObservedClusters().stream().forEach(cluster -> {
      JsonArray scrapeConfigs = receivers
          .getObjectOrPut(getPrometheusClusterReceiver(cluster))
          .getObjectOrPut("config")
          .getArrayOrPut("scrape_configs");
      appendPatroniToScrapeConfigs(cluster, scrapeConfigs);
      appendEnvoyToScrapeConfigs(cluster, scrapeConfigs);
      appendPostgresExporterToScrapeConfigs(cluster, scrapeConfigs);
    });

    final JsonObject pipelines = otelConfig
        .getObjectOrPut("service")
        .getObjectOrPut("pipelines");
    pipelines.streamObjectEntries()
        .forEach(entry -> {
          pipelines.getObjectOrPut(entry.getKey()).put(
              "receivers",
              entry.getValue()
              .getArrayOrPut("receivers")
              .stream()
              .filter(String.class::isInstance)
              .map(String.class::cast)
              .filter(pipelineReceiver -> receivers.containsKey(pipelineReceiver))
              .toList());
          if (pipelines.getObjectOrPut(entry.getKey()).getArray("receivers").isEmpty()) {
            pipelines.getObjectOrPut(entry.getKey()).put("receivers", List.of("nop"));
            receivers.getObjectOrPut("nop");
          }
        });

    if (LOGGER.isTraceEnabled()) {
      otelConfig
          .getObjectOrPut("service")
          .getObjectOrPut("telemetry")
          .getObjectOrPut("logs")
          .put("level", "debug");
    }

    final Map<String, String> data = new HashMap<>();
    data.put(ConfigPath.COLLECTOR_CONFIG_PATH.filename(),
        Unchecked.supplier(() -> yamlMapper.writeValueAsString(
            otelConfig)).get());
    data.putAll(getConfigTemplates());

    return Stream.of(new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withData(data)
        .build());
  }

  private String getPrometheusClusterReceiver(ObservedClusterContext cluster) {
    return "prometheus" + Optional.of(cluster.getCluster().getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getObservability)
        .map(StackGresClusterObservability::getReceiver)
        .map(receiver -> "/" + receiver)
        .orElse("");
  }

  private void appendPatroniToScrapeConfigs(ObservedClusterContext cluster, JsonArray scrapeConfigs) {
    addOrOverwriteScrapeConfig(
        cluster, scrapeConfigs, "patroni", "/metrics", EnvoyUtil.PATRONI_ENTRY_PORT);
  }

  private void appendEnvoyToScrapeConfigs(ObservedClusterContext cluster, JsonArray scrapeConfigs) {
    addOrOverwriteScrapeConfig(
        cluster, scrapeConfigs, "envoy", "/stats/prometheus", EnvoyUtil.ENVOY_PORT);
  }

  private void appendPostgresExporterToScrapeConfigs(ObservedClusterContext cluster, JsonArray scrapeConfigs) {
    addOrOverwriteScrapeConfig(
        cluster, scrapeConfigs, "postgres-exporter", "/metrics", PostgresExporter.POSTGRES_EXPORTER_PORT);
  }

  private void addOrOverwriteScrapeConfig(
      ObservedClusterContext cluster,
      JsonArray scrapeConfigs,
      String suffix,
      String metricsPath,
      Integer metricsPort) {
    final String jobName =
        cluster.getCluster().getMetadata().getNamespace()
        + "-" + cluster.getCluster().getMetadata().getName()
        + "-" + suffix;
    final JsonObject scrapeConfig = scrapeConfigs.streamObjects()
        .filter(config -> Objects.equals(config.get("job_name"), jobName))
        .findFirst()
        .orElseGet(JsonObject::new);
    if (scrapeConfigs.streamObjects()
        .noneMatch(config -> Objects.equals(config.get("job_name"), jobName))) {
      scrapeConfigs.add(scrapeConfig);
    }
    scrapeConfig.put("job_name", jobName);
    scrapeConfig.put("metrics_path", metricsPath);
    scrapeConfig.put("static_configs", new JsonArray());
    JsonArray staticConfigs = scrapeConfig.getArray("static_configs");
    cluster.getPodIps().stream()
        .map(podIp -> new JsonObject(Map.of("targets", new JsonArray(List.of(podIp + ":" + metricsPort)))))
        .forEach(staticConfigs::add);
  }

}
