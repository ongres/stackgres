/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Sidecar(StackGresContainer.ENVOY)
@OperatorVersionBinder
@RunningContainer(StackGresContainer.ENVOY)
public class Envoy implements ContainerFactory<ClusterContainerContext>,
    VolumeFactory<StackGresClusterContext> {

  public static final String POD_MONITOR = "-stackgres-envoy";
  public static final String SERVICE = "-envoyexp";

  protected static final Logger ENVOY_LOGGER = LoggerFactory.getLogger("io.stackgres.envoy");
  protected static final Map<String, Integer> PORT_MAPPING =
      Map.of(
          "postgres_entry_port", EnvoyUtil.PG_ENTRY_PORT,
          "postgres_repl_entry_port", EnvoyUtil.PG_REPL_ENTRY_PORT,
          "babelfish_entry_port", EnvoyUtil.BF_ENTRY_PORT,
          "patroni_entry_port", EnvoyUtil.PATRONI_ENTRY_PORT,
          "postgres_pool_port", EnvoyUtil.PG_POOL_PORT,
          "postgres_port", EnvoyUtil.PG_PORT,
          "babelfish_port", EnvoyUtil.BF_PORT,
          "patroni_port", EnvoyUtil.PATRONI_PORT,
          "envoy_port", EnvoyUtil.ENVOY_PORT);

  protected final YAMLMapper yamlMapper;
  protected final LabelFactoryForCluster labelFactory;

  private final ObjectMapper objectMapper;
  private final ContainerUserOverrideMounts containerUserOverrideMounts;

  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider,
      ObjectMapper jsonMapper,
      LabelFactoryForCluster labelFactory,
      ContainerUserOverrideMounts containerUserOverrideMounts) {
    this.yamlMapper = yamlMapperProvider.get();
    this.labelFactory = labelFactory;
    this.objectMapper = jsonMapper;
    this.containerUserOverrideMounts = containerUserOverrideMounts;
  }

  @Override
  public boolean isActivated(ClusterContainerContext context) {
    return !Optional.of(context.getClusterContext().getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableEnvoy)
        .orElse(false);
  }

  public static String serviceName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + SERVICE);
  }

  public static String podMonitorName(StackGresClusterContext clusterContext) {
    String namespace = clusterContext.getSource().getMetadata().getNamespace();
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(namespace + "-" + name + POD_MONITOR);
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of(
        StackGresContext.ENVOY_VERSION_KEY,
        StackGresComponent.ENVOY.get(context.getClusterContext().getCluster())
        .getLatestVersion());
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Seq.<VolumePair>of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build());
  }

  protected Volume buildVolume(StackGresClusterContext context) {
    final String clusterName = context.getSource().getMetadata().getName();
    return new VolumeBuilder()
        .withName(StackGresVolume.ENVOY.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withDefaultMode(0440)
            .withName(StackGresVolume.ENVOY.getResourceName(clusterName))
            .build())
        .build();
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(StackGresVolume.ENVOY.getName())
        .withImage(StackGresComponent.ENVOY.get(context.getClusterContext().getCluster())
            .getLatestImageName())
        .withImagePullPolicy(getDefaultPullPolicy())
        .withVolumeMounts(new VolumeMountBuilder()
            .withName(StackGresVolume.ENVOY.getName())
            .withMountPath("/etc/envoy")
            .withReadOnly(true)
            .build()
        )
        .addAllToVolumeMounts(getVolumeMounts(context))
        .withPorts(getContainerPorts(context.getClusterContext().getCluster()))
        .withCommand("/usr/local/bin/envoy")
        .withArgs(Seq.of("-c", "/etc/envoy/envoy.json")
            .append(Seq.of(ENVOY_LOGGER.isTraceEnabled())
                .filter(traceEnabled -> traceEnabled)
                .map(traceEnabled -> ImmutableList.of("-l", "debug"))
                .flatMap(List::stream))
            .toArray(String[]::new));
    return container.build();
  }

  private List<ContainerPort> getContainerPorts(StackGresCluster cluster) {
    if (getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH) {
      return List.of(
          new ContainerPortBuilder()
              .withProtocol("TCP")
              .withName(EnvoyUtil.POSTGRES_PORT_NAME)
              .withContainerPort(EnvoyUtil.PG_ENTRY_PORT).build(),
          new ContainerPortBuilder()
              .withProtocol("TCP")
              .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
              .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build(),
          new ContainerPortBuilder()
              .withProtocol("TCP")
              .withName(EnvoyUtil.BABELFISH_PORT_NAME)
              .withContainerPort(EnvoyUtil.BF_ENTRY_PORT).build(),
          new ContainerPortBuilder()
              .withProtocol("TCP")
              .withName(EnvoyUtil.ENVOY_PORT_NAME)
              .withContainerPort(EnvoyUtil.ENVOY_PORT).build(),
          new ContainerPortBuilder()
              .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
              .withProtocol("TCP")
              .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
              .build());
    }
    return List.of(
        new ContainerPortBuilder()
            .withProtocol("TCP")
            .withName(EnvoyUtil.POSTGRES_PORT_NAME)
            .withContainerPort(EnvoyUtil.PG_ENTRY_PORT).build(),
        new ContainerPortBuilder()
            .withProtocol("TCP")
            .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
            .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build(),
        new ContainerPortBuilder()
            .withProtocol("TCP")
            .withName(EnvoyUtil.ENVOY_PORT_NAME)
            .withContainerPort(EnvoyUtil.ENVOY_PORT).build(),
        new ContainerPortBuilder()
            .withName(EnvoyUtil.PATRONI_RESTAPI_PORT_NAME)
            .withProtocol("TCP")
            .withContainerPort(EnvoyUtil.PATRONI_ENTRY_PORT)
            .build());
  }

  private HasMetadata buildSource(StackGresClusterContext context) {
    final StackGresCluster stackGresCluster = context.getSource();

    final ObjectNode envoyConfig;
    try {
      envoyConfig = (ObjectNode) yamlMapper
          .readTree(Envoy.class.getResource("/envoy/envoy.yaml"));
    } catch (Exception ex) {
      throw new IllegalArgumentException("couldn't read envoy config file", ex);
    }
    final ObjectNode envoyConfigLds;
    try {
      envoyConfigLds = (ObjectNode) yamlMapper
          .readTree(Envoy.class.getResource("/envoy/envoy-lds.yaml"));
    } catch (Exception ex) {
      throw new IllegalArgumentException("couldn't read envoy config file", ex);
    }
    final ObjectNode envoyConfigCds;
    try {
      envoyConfigCds = (ObjectNode) yamlMapper
          .readTree(Envoy.class.getResource("/envoy/envoy-cds.yaml"));
    } catch (Exception ex) {
      throw new IllegalArgumentException("couldn't read envoy config file", ex);
    }

    Optional.of(envoyConfig.get("admin").get("address").get("socket_address"))
        .map(ObjectNode.class::cast)
        .filter(socketAddress -> socketAddress.has("port_value")
            && socketAddress.get("port_value").asText().startsWith("$"))
        .ifPresent(socketAddress -> socketAddress.put("port_value",
            Optional.ofNullable(
                PORT_MAPPING.get(socketAddress
                .get("port_value")
                .asText()
                .substring(1)))
            .orElseThrow(() -> new IllegalArgumentException(
                "Can not replace value " + socketAddress.get("port_value").asText()
                .substring(1) + " of field"
                + " .admin.address.socket_address.port_value"
                + " in Envoy configuration"))));

    Seq.seq(envoyConfigLds.get("resources"))
        .map(listener -> listener
            .get("address")
            .get("socket_address"))
        .cast(ObjectNode.class)
        .filter(socketAddress -> socketAddress.has("port_value")
            && socketAddress.get("port_value").asText().startsWith("$"))
        .forEach(socketAddress -> socketAddress.put("port_value",
            Optional.ofNullable(
                PORT_MAPPING.get(socketAddress
                .get("port_value")
                .asText()
                .substring(1)))
            .orElseThrow(() -> new IllegalArgumentException(
                "Can not replace value " + socketAddress.get("port_value").asText()
                .substring(1) + " of field"
                + " .resources[].address.socket_address.port_value"
                + " in Envoy configuration"))));

    Seq.seq(envoyConfigCds.get("resources"))
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
        .filter(socketAddress -> socketAddress.has("port_value")
            && socketAddress.get("port_value").asText().startsWith("$"))
        .forEach(socketAddress -> socketAddress.put("port_value",
            Optional.ofNullable(PORT_MAPPING.get(socketAddress
                .get("port_value")
                .asText()
                .substring(1)))
            .orElseThrow(() -> new IllegalArgumentException(
                "Can not replace value " + socketAddress.get("port_value").asText()
                .substring(1) + " of field"
                + " .resources[].load_assignment.endpoints[].lb_endpoints[]"
                  + ".address.socket_address.port_value"
                + " in Envoy configuration"))));

    setupPgBouncer(stackGresCluster, envoyConfigLds, envoyConfigCds);

    setupSsl(stackGresCluster, envoyConfigLds);

    setupBabelfish(context, envoyConfigCds);

    final Map<String, String> data;
    try {
      data = Map.of(
          "envoy.json",
          objectMapper.writeValueAsString(envoyConfig),
          "envoy-lds.json",
          objectMapper.writeValueAsString(envoyConfigLds),
          "envoy-cds.json",
          objectMapper.writeValueAsString(envoyConfigCds));
    } catch (Exception ex) {
      throw new IllegalArgumentException("couldn't parse envoy config file", ex);
    }

    String namespace = stackGresCluster.getMetadata().getNamespace();
    String clusterName = stackGresCluster.getMetadata().getName();
    String configMapName = StackGresVolume.ENVOY.getResourceName(clusterName);

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(labelFactory.genericLabels(stackGresCluster))
        .endMetadata()
        .withData(data)
        .build();
  }

  private void setupPgBouncer(final StackGresCluster stackGresCluster,
      final ObjectNode envoyConfigLds, final ObjectNode envoyConfigCds) {
    boolean disablePgBouncer = Optional
        .ofNullable(stackGresCluster.getSpec())
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableConnectionPooling)
        .orElse(false);
    final String postgresEntryClusterName;
    if (disablePgBouncer) {
      Seq.seq(envoyConfigCds.get("resources"))
          .zipWithIndex()
          .filter(cluster -> cluster.v1.has("name")
              && cluster.v1.get("name").asText().equals("postgres_cluster_pool"))
          .findFirst()
          .ifPresent(cluster -> ((ArrayNode) envoyConfigCds.get("resources"))
              .remove(cluster.v2.intValue()));

      postgresEntryClusterName = "postgres_cluster";
    } else {
      postgresEntryClusterName = "postgres_cluster_pool";
    }
    Seq.seq(envoyConfigLds.get("resources"))
        .flatMap(listener -> Seq.seq(listener.get("filter_chains").elements()))
        .flatMap(filterChain -> Seq.seq(filterChain.get("filters").elements()))
        .map(filter -> filter
            .get("typed_config"))
        .cast(ObjectNode.class)
        .filter(typedConfig -> typedConfig.has("cluster")
            && typedConfig.get("cluster").asText().equals("$postgres_entry_cluster_name"))
        .forEach(typedConfig -> typedConfig.put("cluster", postgresEntryClusterName));
  }

  private void setupSsl(final StackGresCluster stackGresCluster,
      final ObjectNode envoyConfigLds) {
    boolean enableSsl = Optional
        .ofNullable(stackGresCluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false);
    Seq.seq(envoyConfigLds.get("resources"))
        .flatMap(listener -> Seq.seq(listener.get("filter_chains").elements()))
        .flatMap(filterChain -> Seq.seq(filterChain.get("filters").elements()))
        .map(filter -> filter
            .get("typed_config"))
        .cast(ObjectNode.class)
        .filter(typedConfig -> typedConfig.has("terminate_ssl")
            && typedConfig.get("terminate_ssl").asText().startsWith("$"))
        .forEach(typedConfig -> typedConfig.put("terminate_ssl", enableSsl));
    if (!enableSsl) {
      Seq.seq(envoyConfigLds.get("resources"))
          .flatMap(listener -> Seq.seq(listener.get("filter_chains").elements()))
          .filter(filterChain -> filterChain.has("transport_socket")
              && filterChain.get("transport_socket").has("name")
              && filterChain.get("transport_socket").get("name").asText()
              .equals("starttls"))
          .cast(ObjectNode.class)
          .forEach(filterChain -> filterChain.remove("transport_socket"));
    }
  }

  private void setupBabelfish(StackGresClusterContext context, final ObjectNode envoyConfigCds) {
    boolean disableBabelfish =
        getPostgresFlavorComponent(context.getCluster()) != StackGresComponent.BABELFISH;
    if (disableBabelfish) {
      Seq.seq(envoyConfigCds.get("resources"))
          .zipWithIndex()
          .filter(cluster -> cluster.v1.has("name")
              && cluster.v1.get("name").asText().equals("babelfish_cluster"))
          .findFirst()
          .ifPresent(cluster -> ((ArrayNode) envoyConfigCds.get("resources"))
              .remove(cluster.v2.intValue()));
    }
  }

  private List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    return Seq.seq(containerUserOverrideMounts.getVolumeMounts(context))
        .append(Seq.of(
                new VolumeMountBuilder()
                .withName(StackGresVolume.POSTGRES_SSL.getName())
                .withMountPath(ClusterPath.SSL_PATH.path())
                .withReadOnly(true)
                .build()))
        .toList();
  }

}
