/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgbouncer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ResourceUtils;
import io.stackgres.common.sgcluster.StackGresCluster;
import io.stackgres.sidecars.Sidecar;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;
import io.stackgres.sidecars.pgbouncer.parameters.Blacklist;
import io.stackgres.sidecars.pgbouncer.parameters.DefaultValues;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgBouncer implements Sidecar {

  private static final Logger LOGGER = LoggerFactory.getLogger(PgBouncer.class);

  private static final String NAME = "pgbouncer";
  private static final String IMAGE_PREFIX = "docker.io/ongres/pgbouncer:";
  private static final String DEFAULT_VERSION = "1.11";

  private final String clusterName;
  private final String configMapName;
  private final Supplier<KubernetesClient> kubernetesClientSupplier;

  /**
   * Create a {@code PgBouncer} instance.
   */
  public PgBouncer(String clusterName, Supplier<KubernetesClient> kubernetesClientSupplier) {
    this.clusterName = clusterName;
    this.configMapName = clusterName + "-pgbouncer-config";
    this.kubernetesClientSupplier = kubernetesClientSupplier;
  }

  @Override
  public Container create(StackGresCluster resource) {
    Optional<StackGresPgbouncerConfig> config = getPgbouncerConfig(resource);
    final String pgbouncerVersion = config.map(c -> c.getSpec().getPgbouncerVersion())
        .orElse(DEFAULT_VERSION);

    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")
        .build();

    VolumeMount pgbouncerConfig = new VolumeMountBuilder()
        .withName(NAME)
        .withMountPath("/etc/pgbouncer")
        .withReadOnly(Boolean.TRUE)
        .build();

    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(IMAGE_PREFIX + pgbouncerVersion)
        .withImagePullPolicy("Always")
        .withPorts(new ContainerPortBuilder().withContainerPort(6432).build())
        .withVolumeMounts(pgSocket, pgbouncerConfig);

    return container.build();
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<HasMetadata> createDependencies(StackGresCluster resource) {
    Optional<StackGresPgbouncerConfig> config = getPgbouncerConfig(resource);
    Map<String, String> newParams = config.map(c -> c.getSpec().getPgbouncerConf())
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
        // .withAnnotations(ImmutableMap.of("VolumeMount", "true"))
        .withName(configMapName)
        .withLabels(ResourceUtils.defaultLabels(clusterName))
        .endMetadata()
        .withData(data)
        .build();

    return ImmutableList.of(cm);
  }

  private Optional<StackGresPgbouncerConfig> getPgbouncerConfig(StackGresCluster resource) {
    final String namespace = resource.getMetadata().getNamespace();
    final String pgbouncerConfig = resource.getSpec().getPgbouncerConfig();
    LOGGER.debug("PgbouncerConfig Name: {}", pgbouncerConfig);
    if (pgbouncerConfig != null) {
      try (KubernetesClient client = kubernetesClientSupplier.get()) {
        Optional<CustomResourceDefinition> crd =
            ResourceUtils.getCustomResource(client, StackGresPgbouncerConfigDefinition.NAME);
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
    }
    return Optional.empty();
  }

}
