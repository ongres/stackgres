/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.envoy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.node.ArrayNode;
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
import io.stackgres.operator.app.YamlMapperProvider;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresSidecarTransformer;
import io.stackgres.operator.controller.ResourceGeneratorContext;
import io.stackgres.operator.customresource.prometheus.Endpoint;
import io.stackgres.operator.customresource.prometheus.NamespaceSelector;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorDefinition;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorSpec;
import io.stackgres.operator.resource.ResourceUtil;

import org.jooq.lambda.Seq;

@Singleton
@Sidecar(Envoy.NAME)
public class Envoy
    implements StackGresSidecarTransformer<Void, StackGresClusterContext> {

  public static final String SERVICE_MONITOR = "-stackgres-envoy";
  public static final String SERVICE = "-prometheus-envoy";

  public static final int PG_ENTRY_PORT = 5432;
  public static final int PG_RAW_ENTRY_PORT = 5433;
  public static final int PG_PORT = 5434;
  public static final int PG_RAW_PORT = 5435;
  public static final String NAME = "envoy";

  private static final String IMAGE_NAME = "docker.io/envoyproxy/envoy:v%s";
  private static final String DEFAULT_VERSION = "1.12.1";
  private static final String CONFIG_SUFFIX = "-envoy-config";
  private static final ImmutableMap<String, Integer> LISTEN_SOCKET_ADDRESS_PORT_MAPPING =
      ImmutableMap.of(
          "postgres_entry_port", PG_ENTRY_PORT,
          "postgres_raw_entry_port", PG_RAW_ENTRY_PORT);
  private static final ImmutableMap<String, Integer> CLUSTER_SOCKET_ADDRESS_PORT_MAPPING =
      ImmutableMap.of(
          "postgres_port", PG_PORT,
          "postgres_raw_port", PG_RAW_PORT);

  final YamlMapperProvider yamlMapperProvider;

  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider) {
    this.yamlMapperProvider = yamlMapperProvider;
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + CONFIG_SUFFIX);
  }

  public static String serviceName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + SERVICE);
  }

  public static String serviceMonitorName(StackGresClusterContext clusterContext) {
    String namespace = clusterContext.getCluster().getMetadata().getNamespace();
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(namespace + "-" + name + SERVICE_MONITOR);
  }

  @Override
  public Container getContainer(ResourceGeneratorContext<StackGresClusterContext> context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(String.format(IMAGE_NAME, DEFAULT_VERSION))
        .withImagePullPolicy("Always")
        .withVolumeMounts(new VolumeMountBuilder()
            .withName(NAME)
            .withMountPath("/etc/envoy")
            .withNewReadOnly(true)
            .build())
        .withPorts(
            new ContainerPortBuilder().withContainerPort(PG_ENTRY_PORT).build(),
            new ContainerPortBuilder().withContainerPort(PG_RAW_ENTRY_PORT).build())
        .withCommand("/usr/local/bin/envoy")
        .withArgs("-c", "/etc/envoy/default_envoy.yaml", "-l", "debug");

    return container.build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(
      ResourceGeneratorContext<StackGresClusterContext> context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context.getContext()))
            .build())
        .build());
  }

  @Override
  public List<HasMetadata> getResources(ResourceGeneratorContext<StackGresClusterContext> context) {

    final String envoyConfPath;
    if (context.getContext().getCluster().getSpec()
        .getSidecars().contains("connection-pooling")) {
      envoyConfPath = "/envoy/default_envoy.yaml";
    } else {
      envoyConfPath = "/envoy/envoy_nopgbouncer.yaml";
    }

    YAMLMapper yamlMapper = yamlMapperProvider.yamlMapper();
    final ObjectNode envoyConfig;
    try {
      envoyConfig = (ObjectNode) yamlMapper
          .readTree(getClass().getResource(envoyConfPath));
    } catch (Exception ex) {
      throw new IllegalStateException("couldn't read envoy config file", ex);
    }

    Seq.seq((ArrayNode) envoyConfig.get("static_resources").get("listeners"))
        .map(listener -> listener
            .get("address")
            .get("socket_address"))
        .cast(ObjectNode.class)
        .forEach(socketAddress -> socketAddress.put("port_value",
            LISTEN_SOCKET_ADDRESS_PORT_MAPPING.get(socketAddress
                .get("port_value")
                .asText())));

    Seq.seq((ArrayNode) envoyConfig.get("static_resources").get("clusters"))
        .map(cluster -> cluster
            .get("hosts")
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

    String namespace = context.getContext().getCluster().getMetadata().getNamespace();
    String configMapName = configName(context.getContext());
    ImmutableList.Builder<HasMetadata> resourcesBuilder = ImmutableList.builder();

    ConfigMap cm = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(ResourceUtil.clusterLabels(context.getContext().getCluster()))
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(
            context.getContext().getCluster())))
        .endMetadata()
        .withData(data)
        .build();
    resourcesBuilder.add(cm);

    final Map<String, String> defaultLabels = ResourceUtil.clusterLabels(
        context.getContext().getCluster());
    Map<String, String> labels = new ImmutableMap.Builder<String, String>()
        .putAll(ResourceUtil.clusterCrossNamespaceLabels(
            context.getContext().getCluster()))
        .build();

    Optional<Prometheus> prometheus = context.getContext().getPrometheus();
    resourcesBuilder.add(
        new ServiceBuilder()
            .withNewMetadata()
            .withNamespace(context.getContext().getCluster().getMetadata().getNamespace())
            .withName(serviceName(context.getContext()))
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(labels)
                .put("container", NAME)
                .build())
            .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(
                context.getContext().getCluster())))
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
          serviceMonitor.setKind(ServiceMonitorDefinition.KIND);
          serviceMonitor.setApiVersion(ServiceMonitorDefinition.APIVERSION);
          serviceMonitor.setMetadata(new ObjectMetaBuilder()
              .withNamespace(pi.getNamespace())
              .withName(serviceMonitorName(context.getContext()))
              .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(
                  context.getContext().getCluster())))
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

    return resourcesBuilder.build();
  }

}
