/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.customresource.prometheus.Endpoint;
import io.stackgres.operator.customresource.prometheus.NamespaceSelector;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorSpec;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Sidecar(Envoy.NAME)
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
@RunningContainer(order = 1)
public class Envoy implements ContainerFactory<StackGresClusterContext>,
    ResourceGenerator<StackGresClusterContext> {

  public static final String SERVICE_MONITOR = "-stackgres-envoy";
  public static final String SERVICE = "-prometheus-envoy";

  public static final String NAME = StackgresClusterContainers.ENVOY;

  private static final Logger ENVOY_LOGGER = LoggerFactory.getLogger("io.stackgres.envoy");
  private static final String CONFIG_SUFFIX = "-envoy-config";
  private static final ImmutableMap<String, Integer> LISTEN_SOCKET_ADDRESS_PORT_MAPPING =
      ImmutableMap.of(
          "postgres_entry_port", EnvoyUtil.PG_ENTRY_PORT,
          "postgres_repl_entry_port", EnvoyUtil.PG_REPL_ENTRY_PORT,
          "patroni_entry_port", EnvoyUtil.PATRONI_ENTRY_PORT);
  private static final ImmutableMap<String, Integer> CLUSTER_SOCKET_ADDRESS_PORT_MAPPING =
      ImmutableMap.of(
          "postgres_pool_port", EnvoyUtil.PG_POOL_PORT,
          "postgres_port", EnvoyUtil.PG_PORT,
          "patroni_port", EnvoyUtil.PATRONI_PORT);

  private final YamlMapperProvider yamlMapperProvider;

  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider,
               LabelFactory<StackGresCluster> labelFactory) {
    this.yamlMapperProvider = yamlMapperProvider;
    this.labelFactory = labelFactory;
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContext context) {
    return ImmutableMap.of(
        StackGresContext.ENVOY_VERSION_KEY,
        StackGresComponent.ENVOY.findLatestVersion());
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + CONFIG_SUFFIX);
  }

  public static String serviceName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + SERVICE);
  }

  public static String serviceMonitorName(StackGresClusterContext clusterContext) {
    String namespace = clusterContext.getSource().getMetadata().getNamespace();
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(namespace + "-" + name + SERVICE_MONITOR);
  }

  @Override
  public Container getContainer(StackGresClusterContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(StackGresComponent.ENVOY.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withVolumeMounts(new VolumeMountBuilder()
            .withName(NAME)
            .withMountPath("/etc/envoy")
            .withReadOnly(Boolean.TRUE)
            .build())
        .addAllToVolumeMounts(ClusterStatefulSetVolumeConfig.USER.volumeMounts(context))
        .withPorts(
            new ContainerPortBuilder().withContainerPort(EnvoyUtil.PG_ENTRY_PORT).build(),
            new ContainerPortBuilder().withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build())
        .withCommand("/usr/local/bin/envoy")
        .withArgs(Seq.of("-c", "/etc/envoy/default_envoy.yaml",
            "--bootstrap-version", "2")
            .append(Seq.of(ENVOY_LOGGER.isTraceEnabled())
                .filter(traceEnabled -> traceEnabled)
                .map(traceEnabled -> ImmutableList.of("-l", "debug"))
                .flatMap(List::stream))
            .toArray(String[]::new));
    return container.build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(
      StackGresClusterContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build());
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {

    final StackGresCluster stackGresCluster = context.getSource();
    boolean disablePgBouncer = Optional
        .ofNullable(stackGresCluster.getSpec())
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getDisableConnectionPooling)
        .orElse(false);
    final String envoyConfPath;
    if (disablePgBouncer) {
      envoyConfPath = "/envoy/envoy_nopgbouncer.yaml";
    } else {
      envoyConfPath = "/envoy/default_envoy.yaml";
    }

    YAMLMapper yamlMapper = yamlMapperProvider.yamlMapper();
    final ObjectNode envoyConfig;
    try {
      envoyConfig = (ObjectNode) yamlMapper
          .readTree(Envoy.class.getResource(envoyConfPath));
    } catch (Exception ex) {
      throw new IllegalStateException("couldn't read envoy config file", ex);
    }

    Seq.seq(envoyConfig.get("static_resources").get("listeners"))
        .map(listener -> listener
            .get("address")
            .get("socket_address"))
        .cast(ObjectNode.class)
        .forEach(socketAddress -> socketAddress.put("port_value",
            LISTEN_SOCKET_ADDRESS_PORT_MAPPING.get(socketAddress
                .get("port_value")
                .asText())));

    Seq.seq(envoyConfig.get("static_resources").get("clusters"))
        .flatMap(cluster -> Seq.seq(cluster
            .get("load_assignment")
            .get("endpoints").elements()))
        .flatMap(endpoint -> Seq.seq(endpoint
            .get("lb_endpoints").elements()))
        .map(endpoint -> endpoint
            .get("endpoint")
            .get("address")
            .get("socket_address"))
        .cast(ObjectNode.class)
        .forEach(socketAddress -> socketAddress.put("port_value",
            CLUSTER_SOCKET_ADDRESS_PORT_MAPPING.get(socketAddress
                .get("port_value")
                .asText())));

    final Map<String, String> data;
    try {
      data = ImmutableMap.of("default_envoy.yaml",
          yamlMapper.writeValueAsString(envoyConfig));
    } catch (Exception ex) {
      throw new IllegalStateException("couldn't parse envoy config file", ex);
    }

    String namespace = stackGresCluster.getMetadata().getNamespace();
    String configMapName = configName(context);
    ImmutableList.Builder<HasMetadata> resourcesBuilder = ImmutableList.builder();

    ConfigMap cm = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(labelFactory.clusterLabels(stackGresCluster))
        .endMetadata()
        .withData(data)
        .build();
    resourcesBuilder.add(cm);

    final Map<String, String> defaultLabels = labelFactory.clusterLabels(stackGresCluster);
    Map<String, String> labels = new ImmutableMap.Builder<String, String>()
        .putAll(labelFactory.clusterCrossNamespaceLabels(stackGresCluster))
        .build();

    Optional<Prometheus> prometheus = context.getPrometheus();
    resourcesBuilder.add(
        new ServiceBuilder()
            .withNewMetadata()
            .withNamespace(stackGresCluster.getMetadata().getNamespace())
            .withName(serviceName(context))
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(labels)
                .put("container", NAME)
                .build())
            .endMetadata()
            .withSpec(new ServiceSpecBuilder()
                .withSelector(defaultLabels)
                .withPorts(new ServicePortBuilder()
                    .withName(NAME)
                    .withPort(8001)
                    .build())
                .build())
            .build());

    prometheus.ifPresent(c -> {
      if (Optional.ofNullable(c.getCreateServiceMonitor()).orElse(false)) {
        c.getPrometheusInstallations().forEach(pi -> {
          ServiceMonitor serviceMonitor = new ServiceMonitor();
          serviceMonitor.setMetadata(new ObjectMetaBuilder()
              .withNamespace(pi.getNamespace())
              .withName(serviceMonitorName(context))
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
          endpoint.setPort(NAME);
          endpoint.setPath("/stats/prometheus");
          spec.setEndpoints(Collections.singletonList(endpoint));

          resourcesBuilder.add(serviceMonitor);

        });
      }
    });

    return Seq.seq(resourcesBuilder.build());
  }
}
