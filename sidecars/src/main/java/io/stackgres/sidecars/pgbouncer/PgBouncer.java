/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgbouncer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.Sidecar;
import io.stackgres.common.StackGresClusterConfig;
import io.stackgres.common.StackGresSidecarTransformer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;
import io.stackgres.sidecars.pgbouncer.parameters.Blacklist;
import io.stackgres.sidecars.pgbouncer.parameters.DefaultValues;

@Sidecar("connection-pooling")
@Singleton
public class PgBouncer implements StackGresSidecarTransformer<StackGresPgbouncerConfig> {

  private static final String NAME = "pgbouncer";
  private static final String IMAGE_PREFIX = "docker.io/ongres/pgbouncer:v%s-build-%s";
  private static final String DEFAULT_VERSION = "1.11.0";
  private static final String CONFIG_SUFFIX = "-connection-pooling-config";

  @Override
  public List<HasMetadata> getResources(StackGresClusterConfig config) {
    String name = config.getCluster().getMetadata().getName();
    String namespace = config.getCluster().getMetadata().getNamespace();
    String configMapName = name + CONFIG_SUFFIX;
    Optional<StackGresPgbouncerConfig> pgbouncerConfig = config.getSidecarConfig(this);
    Map<String, String> newParams = pgbouncerConfig.map(c -> c.getSpec().getPgbouncerConf())
        .orElseGet(HashMap::new);
    // Blacklist removal
    for (String bl : Blacklist.getBlacklistParameters()) {
      newParams.remove(bl);
    }
    Map<String, String> params = new HashMap<>(DefaultValues.getDefaultValues());

    for (Map.Entry<String, String> entry : newParams.entrySet()) {
      params.put(entry.getKey(), entry.getValue());
    }

    String configFile = "[databases]\n"
        + " * = \n"
        + "\n"
        + "[pgbouncer]\n"
        + params.entrySet().stream()
        .map(entry -> " " + entry.getKey() + " = " + entry.getValue())
        .collect(Collectors.joining("\n"))
        + "\n";
    Map<String, String> data = ImmutableMap.of("pgbouncer.ini", configFile);

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

  @Override
  public Container getContainer(StackGresClusterConfig config) {
    Optional<StackGresPgbouncerConfig> pgbouncerConfig = config.getSidecarConfig(this);
    final String pgbouncerVersion = pgbouncerConfig.map(c -> c.getSpec().getPgbouncerVersion())
        .orElse(DEFAULT_VERSION);

    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")
        .build();

    VolumeMount pgbouncerIni = new VolumeMountBuilder()
        .withName(NAME)
        .withMountPath("/etc/pgbouncer")
        .withReadOnly(Boolean.TRUE)
        .build();

    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(String.format(IMAGE_PREFIX,
            pgbouncerVersion, StackGresUtil.CONTAINER_BUILD))
        .withImagePullPolicy("Always")
        .withPorts(new ContainerPortBuilder().withContainerPort(6432).build())
        .withVolumeMounts(pgSocket, pgbouncerIni);

    return container.build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(StackGresClusterConfig config) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(config.getCluster().getMetadata().getName() + CONFIG_SUFFIX).build())
        .build());
  }

  @Override
  public Optional<StackGresPgbouncerConfig> getConfig(StackGresCluster cluster,
                                                      KubernetesClient client) throws Exception {
    final String namespace = cluster.getMetadata().getNamespace();
    final String pgbouncerConfig = cluster.getSpec().getConnectionPoolingConfig();
    if (pgbouncerConfig != null) {
      Optional<CustomResourceDefinition> crd =
          ResourceUtil.getCustomResource(client, StackGresPgbouncerConfigDefinition.NAME);
      if (crd.isPresent()) {
        return Optional.ofNullable(client
            .customResources(crd.get(),
                StackGresPgbouncerConfig.class,
                StackGresPgbouncerConfigList.class,
                StackGresPgbouncerConfigDoneable.class)
            .inNamespace(namespace)
            .withName(pgbouncerConfig)
            .get());
      }
    }
    return Optional.empty();
  }

}
