/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.envoy;

import java.util.List;
import java.util.Map;

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
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.app.YamlMapperProvider;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresSidecarTransformer;
import io.stackgres.operator.controller.ResourceGeneratorContext;
import io.stackgres.operator.resource.ResourceUtil;

import org.jooq.lambda.Seq;

@Singleton
@Sidecar("envoy")
public class Envoy implements StackGresSidecarTransformer<CustomResource> {

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

  @Inject
  YamlMapperProvider yamlMapperProvider;

  @Override
  public Container getContainer(ResourceGeneratorContext context) {
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
  public ImmutableList<Volume> getVolumes(ResourceGeneratorContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(context.getClusterConfig().getCluster().getMetadata()
                .getName() + CONFIG_SUFFIX)
            .build())
        .build());
  }

  @Override
  public List<HasMetadata> getResources(ResourceGeneratorContext context) {

    final String envoyConfPath;
    if (context.getClusterConfig().getCluster().getSpec()
        .getSidecars().contains("connection-pooling")) {
      envoyConfPath = "/envoy/default_envoy.yaml";
    } else {
      envoyConfPath = "/envoy/envoy_nopgbouncer.yaml";
    }

    try {
      YAMLMapper yamlMapper = yamlMapperProvider.yamlMapper();
      ObjectNode envoyConfig = (ObjectNode) yamlMapper
          .readTree(getClass().getResource(envoyConfPath));
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

      Map<String, String> data = ImmutableMap.of("default_envoy.yaml",
          yamlMapper.writeValueAsString(envoyConfig));

      String name = context.getClusterConfig().getCluster().getMetadata().getName();
      String namespace = context.getClusterConfig().getCluster().getMetadata().getNamespace();
      String configMapName = name + CONFIG_SUFFIX;

      ConfigMap cm = new ConfigMapBuilder()
          .withNewMetadata()
          .withNamespace(namespace)
          .withName(configMapName)
          .withLabels(ResourceUtil.defaultLabels(name))
          .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(
              context.getClusterConfig().getCluster())))
          .endMetadata()
          .withData(data)
          .build();

      return ImmutableList.of(cm);

    } catch (Exception ex) {
      throw new IllegalStateException("couldn't read envoy config file", ex);
    }

  }
}
