/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ConfigPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.JsonArray;
import io.stackgres.common.crd.JsonObject;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterObservability;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollector;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorReceiver;
import io.stackgres.common.crd.sgconfig.StackGresConfigCollectorReceiverDeployment;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.common.ObservedClusterContext.CollectorPodContext;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesConfigMap;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pgexporter.PostgresExporter;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class CollectorConfigMaps
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
    return CollectorDeployments.name(config);
  }

  public static String receiversName(StackGresConfig config, int index) {
    return CollectorDeployments.receiversName(config, index);
  }

  public static List<List<Tuple2<ObservedClusterContext, CollectorPodContext>>> getObserverdClusterPodsPartitions(
      StackGresConfigContext context) {
    final StackGresConfig config = context.getSource();
    final boolean isReceiversEnabled = Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getReceivers)
        .map(StackGresConfigCollectorReceiver::getEnabled)
        .orElse(false);
    if (!isReceiversEnabled) {
      return List.of();
    }
    final List<Tuple2<ObservedClusterContext, CollectorPodContext>> observedClustersPods =
        context.getObservedClusters().stream()
        .flatMap(cluster -> cluster.getPods().stream().map(pod -> Tuple.tuple(cluster, pod)))
        .toList();
    final long observedClustersPodsSize = observedClustersPods.size();
    final Optional<List<StackGresConfigCollectorReceiverDeployment>> deployments =
        Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getReceivers)
        .map(StackGresConfigCollectorReceiver::getDeployments);
    final long deploymentsSize = deployments
        .map(List::size)
        .map(Integer::longValue)
        .orElse(observedClustersPodsSize);
    return Seq.seq(observedClustersPods)
        .sorted(Comparator.comparing(clusterPod -> Tuple.tuple(
            clusterPod.v2.getCreationTimestamp(),
            clusterPod.v2.getNamespace(),
            clusterPod.v2.getName())))
        .zipWithIndex()
        .grouped(clusterPodIndexed -> deployments
            .flatMap(d -> Seq.seq(d)
                .zipWithIndex()
                .filter(dd -> dd.v1.getSgClusters().stream()
                    .anyMatch(sgCluster -> Objects.equals(
                        sgCluster.getNamespace(),
                        clusterPodIndexed.v1.v1.getCluster().getMetadata().getNamespace())
                        && Objects.equals(
                            sgCluster.getName(),
                            clusterPodIndexed.v1.v1.getCluster().getMetadata().getName())
                        && Optional.ofNullable(sgCluster.getIndexes())
                            .map(indexes -> indexes.stream()
                                .anyMatch(index -> index == ResourceUtil.getIndexFromNameWithIndex(
                                    clusterPodIndexed.v1.v2.getName())))
                            .orElse(true)))
                .findFirst()
                .map(Tuple2::v2))
            .orElseGet(() -> clusterPodIndexed.v2.longValue() * deploymentsSize / observedClustersPodsSize))
        .map(clusterPodsPartition -> clusterPodsPartition.v2().map(Tuple2::v1).toList())
        .toList();
  }

  @Inject
  public CollectorConfigMaps(
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

    ConfigMap collectorConfigMap = getCollectorConfigMap(context);

    return Seq.<HasMetadata>of(collectorConfigMap)
        .append(Seq.seq(getObserverdClusterPodsPartitions(context))
            .zipWithIndex()
            .map(observedClusterPodsPartition -> getCollectorReceiversConfigMap(
                context, observedClusterPodsPartition.v2.intValue(), observedClusterPodsPartition.v1)));
  }

  private ConfigMap getCollectorConfigMap(StackGresConfigContext context) {
    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);
    final Map<String, String> data = new HashMap<>();

    String collectorConfig = getCollectorConfig(context, config);
    data.put(ConfigPath.COLLECTOR_CONFIG_PATH.filename(), collectorConfig);
    data.putAll(getConfigTemplates());

    ConfigMap collectorConfigMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withData(data)
        .build();
    return collectorConfigMap;
  }

  private String getCollectorConfig(StackGresConfigContext context, final StackGresConfig config) {
    JsonObject otelConfig = getBasicCollectorConfig(config);

    Boolean isReceiversEnabled = Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getCollector)
        .map(StackGresConfigCollector::getReceivers)
        .map(StackGresConfigCollectorReceiver::getEnabled)
        .orElse(false);
    if (!isReceiversEnabled) {
      setReceivers(
          otelConfig,
          context.getObservedClusters().stream()
          .flatMap(cluster -> cluster.getPods().stream().map(pod -> Tuple.tuple(cluster, pod)))
          .toList());
    } else {
      JsonObject otlp = otelConfig
          .getObjectOrPut("receivers")
          .getObjectOrPut("otlp");
      otelConfig.put("receivers", new JsonObject());
      otelConfig
          .getObjectOrPut("receivers")
          .put("otlp", otlp);
      final JsonObject pipelines = otelConfig
          .getObjectOrPut("service")
          .getObjectOrPut("pipelines");
      pipelines
          .streamObjectEntries()
          .forEach(entry -> {
            pipelines
                .getObjectOrPut(entry.getKey())
                .put("receivers", List.of("otlp"));
          });
    }
    cleanupPipelinesReceivers(otelConfig);

    String collectorConfig = Unchecked.supplier(() -> yamlMapper.writeValueAsString(
        otelConfig)).get();
    return collectorConfig;
  }

  private ConfigMap getCollectorReceiversConfigMap(
      StackGresConfigContext context,
      int index,
      List<Tuple2<ObservedClusterContext, CollectorPodContext>> clusterPods) {
    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);
    final Map<String, String> data = new HashMap<>();

    String collectorConfig = getCollectorReceiversConfig(context, config, index, clusterPods);
    data.put(ConfigPath.COLLECTOR_CONFIG_PATH.filename(), collectorConfig);

    ConfigMap collectorConfigMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(receiversName(config, index))
        .withLabels(labels)
        .endMetadata()
        .withData(data)
        .build();
    return collectorConfigMap;
  }

  private String getCollectorReceiversConfig(
      StackGresConfigContext context,
      StackGresConfig config,
      int index,
      List<Tuple2<ObservedClusterContext, CollectorPodContext>> clusterPods) {
    JsonObject otelConfig = getBasicCollectorConfig(config);

    setReceivers(otelConfig, clusterPods);
    cleanupPipelinesReceivers(otelConfig);
    final JsonObject otlp = otelConfig
        .getObjectOrPut("exporters")
        .getObjectOrPut("otlp");
    final JsonObject debug = otelConfig
        .getObjectOrPut("exporters")
        .getObject("debug");
    otelConfig.put("exporters", new JsonObject());
    otelConfig
        .getObjectOrPut("exporters")
        .put("otlp", otlp);
    otelConfig
        .getObjectOrPut("exporters")
        .put("debug", debug);
    final JsonObject pipelines = otelConfig
        .getObjectOrPut("service")
        .getObjectOrPut("pipelines");
    pipelines
        .streamObjectEntries()
        .forEach(entry -> {
          pipelines
              .getObjectOrPut(entry.getKey())
              .put("exporters", debug == null ? List.of("otlp") : List.of("otlp", "debug"));
        });

    String collectorConfig = Unchecked.supplier(
        () -> yamlMapper.writeValueAsString(otelConfig)).get();
    return collectorConfig;
  }

  private void setReceivers(JsonObject otelConfig,
      List<Tuple2<ObservedClusterContext, CollectorPodContext>> clusterPods) {
    final JsonObject receivers = otelConfig
        .getObjectOrPut("receivers");
    clusterPods.stream()
        .forEach(clusterPod -> {
          JsonArray scrapeConfigs = receivers
              .getObjectOrPut(getPrometheusClusterReceiver(clusterPod.v1))
              .getObjectOrPut("config")
              .getArrayOrPut("scrape_configs");
          appendPatroniToScrapeConfigs(clusterPod.v1, clusterPod.v2, scrapeConfigs);
          appendEnvoyToScrapeConfigs(clusterPod.v1, clusterPod.v2, scrapeConfigs);
          appendPostgresExporterToScrapeConfigs(clusterPod.v1, clusterPod.v2, scrapeConfigs);
        });
  }

  private void cleanupPipelinesReceivers(JsonObject otelConfig) {
    final JsonObject receivers = otelConfig
        .getObjectOrPut("receivers");
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
  }

  private JsonObject getBasicCollectorConfig(StackGresConfig config) {
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

    if (LOGGER.isDebugEnabled()) {
      otelConfig
          .getObjectOrPut("service")
          .getObjectOrPut("telemetry")
          .getObjectOrPut("logs")
          .put("level", "debug");
    }
    if (LOGGER.isTraceEnabled()) {
      JsonObject debug = otelConfig
          .getObjectOrPut("exporters")
          .getObjectOrPut("debug");
      debug.put("verbosity", "detailed");
      debug.put("sampling_initial", 5);
      debug.put("sampling_thereafter", 200);
      otelConfig
          .getObjectOrPut("service")
          .getObjectOrPut("pipelines")
          .streamObjectEntries()
          .forEach(entry -> {
            if (!entry.getValue().getArrayOrPut("exporters").contains("debug")) {
              entry.getValue().getArray("exporters").add("debug");
            }
          });
    }

    return otelConfig;
  }

  private String getPrometheusClusterReceiver(ObservedClusterContext cluster) {
    return "prometheus" + Optional.of(cluster.getCluster().getSpec())
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getObservability)
        .map(StackGresClusterObservability::getReceiver)
        .map(receiver -> "/" + receiver)
        .orElse("");
  }

  private void appendPatroniToScrapeConfigs(
      ObservedClusterContext cluster, CollectorPodContext pod, JsonArray scrapeConfigs) {
    addOrOverwriteScrapeConfig(
        cluster,
        pod,
        scrapeConfigs,
        "patroni",
        StackGresContainer.PATRONI,
        EnvoyUtil.PATRONI_RESTAPI_PORT_NAME,
        "/metrics",
        EnvoyUtil.PATRONI_ENTRY_PORT);
  }

  private void appendEnvoyToScrapeConfigs(
      ObservedClusterContext cluster, CollectorPodContext pod, JsonArray scrapeConfigs) {
    addOrOverwriteScrapeConfig(
        cluster,
        pod,
        scrapeConfigs,
        "envoy",
        StackGresContainer.ENVOY,
        EnvoyUtil.ENVOY_PORT_NAME,
        "/stats/prometheus",
        EnvoyUtil.ENVOY_PORT);
  }

  private void appendPostgresExporterToScrapeConfigs(
      ObservedClusterContext cluster, CollectorPodContext pod, JsonArray scrapeConfigs) {
    addOrOverwriteScrapeConfig(
        cluster,
        pod,
        scrapeConfigs,
        "postgres-exporter",
        StackGresContainer.POSTGRES_EXPORTER,
        PostgresExporter.POSTGRES_EXPORTER_PORT_NAME,
        "/metrics",
        PostgresExporter.POSTGRES_EXPORTER_PORT);
  }

  private void addOrOverwriteScrapeConfig(
      ObservedClusterContext cluster,
      CollectorPodContext pod,
      JsonArray scrapeConfigs,
      String suffix,
      StackGresContainer container,
      String endpoint,
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
    scrapeConfig.put("tls_config", new JsonObject());
    scrapeConfig.getObject("tls_config").put("cert_file", ConfigPath.CERTIFICATE_PATH.path());
    scrapeConfig.getObject("tls_config").put("key_file", ConfigPath.CERTIFICATE_KEY_PATH.path());
    scrapeConfig.put("static_configs", new JsonArray());
    final JsonArray staticConfigs = scrapeConfig.getArray("static_configs");
    final String instance = pod.getIp() + ":" + metricsPort;
    staticConfigs.add(new JsonObject(Map.of(
        "targets", new JsonArray(List.of(instance)),
        "labels", Map.of(
            "container", container.getName(),
            "endpoint", endpoint,
            "instance", instance,
            "namespace", pod.getNamespace(),
            "pod", pod.getName()))));
  }

}
