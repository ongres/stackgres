/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractEnvoy implements ContainerFactory<StackGresClusterContainerContext>,
    VolumeFactory<StackGresClusterContext> {

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
  public AbstractEnvoy(YamlMapperProvider yamlMapperProvider,
                       LabelFactory<StackGresCluster> labelFactory) {
    this.yamlMapperProvider = yamlMapperProvider;
    this.labelFactory = labelFactory;
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
  public Container getContainer(StackGresClusterContainerContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(StackGresComponent.ENVOY.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withVolumeMounts(new VolumeMountBuilder()
            .withName(NAME)
            .withMountPath("/etc/envoy")
            .withReadOnly(true)
            .build()
        )
        .addAllToVolumeMounts(getVolumeMounts(context))
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
  public Map<String, String> getComponentVersions(StackGresClusterContainerContext context) {
    return ImmutableMap.of(
        StackGresContext.ENVOY_VERSION_KEY,
        StackGresComponent.ENVOY.findLatestVersion());
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Seq.<VolumePair>of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build())
        .append(sslVolume(context));
  }

  private Stream<ImmutableVolumePair> sslVolume(StackGresClusterContext context) {
    return Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .filter(ssl -> Optional.ofNullable(ssl.getEnabled()).orElse(false))
        .stream()
        .map(ssl -> ImmutableVolumePair.builder()
            .volume(new VolumeBuilder()
                .withName("ssl")
                .withSecret(new SecretVolumeSourceBuilder()
                    .withSecretName(ssl.getCertificateSecretKeySelector().getName())
                    .withDefaultMode(0400) //NOPMD
                    .withOptional(false)
                    .build())
                .build())
            .build());
  }

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    final String clusterName = context.getSource().getMetadata().getName();
    return new VolumeBuilder()
        .withName(AbstractEnvoy.NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(StatefulSetDynamicVolumes.ENVOY.getResourceName(clusterName))
            .build())
        .build();
  }

  public @NotNull HasMetadata buildSource(StackGresClusterContext context) {

    final StackGresCluster stackGresCluster = context.getSource();
    boolean disablePgBouncer = Optional
        .ofNullable(stackGresCluster.getSpec())
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getDisableConnectionPooling)
        .orElse(false);
    boolean enableSsl = Optional
        .ofNullable(stackGresCluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false);
    final String envoyConfPath;
    if (enableSsl) {
      if (disablePgBouncer) {
        envoyConfPath = "/envoy/envoy_ssl_nopgbouncer.yaml";
      } else {
        envoyConfPath = "/envoy/envoy_ssl.yaml";
      }
    } else {
      if (disablePgBouncer) {
        envoyConfPath = "/envoy/envoy_nopgbouncer.yaml";
      } else {
        envoyConfPath = "/envoy/default_envoy.yaml";
      }
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
    String configMapName = AbstractEnvoy.configName(context);

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(labelFactory.clusterLabels(stackGresCluster))
        .endMetadata()
        .withData(data)
        .build();

  }

  public abstract List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context);

  public abstract String getImageName();

}
