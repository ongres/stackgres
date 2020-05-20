/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.envoy;

import java.util.Collections;
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
import io.stackgres.common.LabelFactory;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.customresource.prometheus.Endpoint;
import io.stackgres.operator.customresource.prometheus.NamespaceSelector;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorDefinition;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorSpec;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Singleton
@Sidecar(Envoy.NAME)
public class Envoy implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String SERVICE_MONITOR = "-stackgres-envoy";
  public static final String SERVICE = "-prometheus-envoy";

  public static final int PG_ENTRY_PORT = 5432;
  public static final int PG_REPL_ENTRY_PORT = 5433;
  public static final int PG_POOL_PORT = 5434;
  public static final int PG_PORT = 5435;
  public static final String NAME = "envoy";

  private static final String IMAGE_NAME = "docker.io/envoyproxy/envoy:v%s";
  private static final String DEFAULT_VERSION = StackGresComponents.get("envoy");
  private static final String CONFIG_SUFFIX = "-envoy-config";
  private static final ImmutableMap<String, Integer> LISTEN_SOCKET_ADDRESS_PORT_MAPPING =
      ImmutableMap.of(
          "postgres_entry_port", PG_ENTRY_PORT,
          "postgres_repl_entry_port", PG_REPL_ENTRY_PORT);
  private static final ImmutableMap<String, Integer> CLUSTER_SOCKET_ADDRESS_PORT_MAPPING =
      ImmutableMap.of(
          "postgres_pool_port", PG_POOL_PORT,
          "postgres_port", PG_PORT);

  private final YamlMapperProvider yamlMapperProvider;

  private final LabelFactoryDelegator factoryDelegator;

  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider, LabelFactoryDelegator factoryDelegator) {
    this.yamlMapperProvider = yamlMapperProvider;
    this.factoryDelegator = factoryDelegator;
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
  public Container getContainer(StackGresGeneratorContext context) {
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
            new ContainerPortBuilder().withContainerPort(PG_REPL_ENTRY_PORT).build())
        .withCommand("/usr/local/bin/envoy")
        .withArgs("-c", "/etc/envoy/default_envoy.yaml", "-l", "debug");

    return container.build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(
      StackGresGeneratorContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context.getClusterContext()))
            .build())
        .build());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {

    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster stackGresCluster = clusterContext.getCluster();
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
          .readTree(getClass().getResource(envoyConfPath));
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

    String namespace = stackGresCluster.getMetadata().getNamespace();
    String configMapName = configName(clusterContext);
    ImmutableList.Builder<HasMetadata> resourcesBuilder = ImmutableList.builder();

    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    ConfigMap cm = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(labelFactory.clusterLabels(stackGresCluster))
        .withOwnerReferences(clusterContext.getOwnerReferences())
        .endMetadata()
        .withData(data)
        .build();
    resourcesBuilder.add(cm);

    final Map<String, String> defaultLabels = labelFactory.clusterLabels(stackGresCluster);
    Map<String, String> labels = new ImmutableMap.Builder<String, String>()
        .putAll(labelFactory.clusterCrossNamespaceLabels(stackGresCluster))
        .build();

    Optional<Prometheus> prometheus = clusterContext.getPrometheus();
    resourcesBuilder.add(
        new ServiceBuilder()
            .withNewMetadata()
            .withNamespace(stackGresCluster.getMetadata().getNamespace())
            .withName(serviceName(clusterContext))
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(labels)
                .put("container", NAME)
                .build())
            .withOwnerReferences(clusterContext.getOwnerReferences())
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
              .withName(serviceMonitorName(clusterContext))
              .withOwnerReferences(clusterContext.getOwnerReferences())
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
