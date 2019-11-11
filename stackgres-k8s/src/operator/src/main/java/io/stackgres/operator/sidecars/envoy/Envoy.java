/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.envoy;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.inject.Singleton;

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
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.common.StackGresSidecarTransformer;
import io.stackgres.operator.resource.ResourceUtil;

@Singleton
@Sidecar("envoy")
public class Envoy implements StackGresSidecarTransformer<CustomResource> {

  public static final int PG_ENTRY_PORT = 5432;
  public static final int REPLICATION_ENTRY_PORT = 5433;
  private static final String NAME = "envoy";
  private static final String IMAGE_NAME = "docker.io/envoyproxy/envoy:v%s";
  private static final String CONFIG_SUFFIX = "-envoy-config";

  @Override
  public Container getContainer(StackGresClusterConfig config) {
    VolumeMount envoyVolume = new VolumeMountBuilder()
        .withName(NAME)
        .withMountPath("/etc/envoy")
        .withNewReadOnly(true)
        .build();

    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(String.format(IMAGE_NAME, config.getCluster().getSpec().getEnvoyVersion()))
        .withImagePullPolicy("Always")
        .withVolumeMounts(envoyVolume)
        .withPorts(
            new ContainerPortBuilder().withContainerPort(PG_ENTRY_PORT).build(),
            new ContainerPortBuilder().withContainerPort(REPLICATION_ENTRY_PORT).build())
        .withCommand("/usr/local/bin/envoy")
        .withArgs("-c", "/etc/envoy/default_envoy.yaml", "-l", "debug");

    return container.build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(StackGresClusterConfig config) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(config.getCluster().getMetadata().getName() + CONFIG_SUFFIX)
            .build())
        .build());
  }

  @Override
  public List<HasMetadata> getResources(StackGresClusterConfig config) {

    String envoyConfPath;
    if (config.getCluster().getSpec().getSidecars().contains("connection-pooling")) {
      envoyConfPath = "envoy/default_envoy.yaml";
    } else {
      envoyConfPath = "envoy/envoy_nopgbouncer.yaml";
    }

    try (InputStream is = ClassLoader
        .getSystemResourceAsStream(envoyConfPath)) {

      if (is == null) {
        throw new IllegalStateException("envoy configuration file not found");
      }

      try (Scanner s = new Scanner(is)) {
        s.useDelimiter("\\A");

        if (!s.hasNext()) {
          throw new IllegalStateException("envoy configuration file not found");
        }

        String envoyConf = s.next();
        Map<String, String> data = ImmutableMap.of("default_envoy.yaml", envoyConf);

        String name = config.getCluster().getMetadata().getName();
        String namespace = config.getCluster().getMetadata().getNamespace();
        String configMapName = name + CONFIG_SUFFIX;

        ConfigMap cm = new ConfigMapBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(configMapName)
            .withLabels(ResourceUtil.defaultLabels(name))
            .endMetadata()
            .withData(data)
            .build();

        return ImmutableList.of(cm);

      }
    } catch (IOException e) {
      throw new IllegalStateException("couldn't read envoy config file", e);
    }

  }
}
