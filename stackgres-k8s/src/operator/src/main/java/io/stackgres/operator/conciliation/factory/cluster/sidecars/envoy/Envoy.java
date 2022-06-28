/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import org.jooq.lambda.Seq;

@Singleton
@Sidecar(StackGresContainer.ENVOY)
@OperatorVersionBinder
@RunningContainer(StackGresContainer.ENVOY)
public class Envoy extends AbstractEnvoy {

  private final ObjectMapper objectMapper;
  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;

  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider,
      ObjectMapper jsonMapper,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      @ProviderName(CONTAINER_USER_OVERRIDE)
      VolumeMountsProvider<ContainerContext> containerUserOverrideMounts) {
    super(yamlMapperProvider, labelFactory);
    this.objectMapper = jsonMapper;
    this.containerUserOverrideMounts = containerUserOverrideMounts;
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(StackGresContainer.ENVOY.getName())
        .withImage(StackGresComponent.ENVOY.get(context.getClusterContext().getCluster())
            .findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withVolumeMounts(new VolumeMountBuilder()
            .withName(StackGresContainer.ENVOY.getName())
            .withMountPath("/etc/envoy")
            .withReadOnly(true)
            .build()
        )
        .addAllToVolumeMounts(getVolumeMounts(context))
        .withPorts(
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build())
        .withCommand("/usr/local/bin/envoy")
        .withArgs(Seq.of("-c", "/etc/envoy/envoy.json")
            .append(Seq.of(ENVOY_LOGGER.isTraceEnabled())
                .filter(traceEnabled -> traceEnabled)
                .map(traceEnabled -> ImmutableList.of("-l", "debug"))
                .flatMap(List::stream))
            .toArray(String[]::new));
    return container.build();
  }

  @Override
  protected Stream<ImmutableVolumePair> buildExtraVolumes(StackGresClusterContext context) {
    return sslVolume(context);
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

  @Override
  protected String getEnvoyConfigPath(final StackGresCluster stackGresCluster,
      boolean disablePgBouncer) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected HasMetadata buildSource(StackGresClusterContext context) {
    final StackGresCluster stackGresCluster = context.getSource();

    YAMLMapper yamlMapper = yamlMapperProvider.get();
    final ObjectNode envoyConfig;
    try {
      envoyConfig = (ObjectNode) yamlMapper
          .readTree(Envoy.class.getResource("/envoy/envoy.yaml"));
    } catch (Exception ex) {
      throw new IllegalStateException("couldn't read envoy config file", ex);
    }

    Seq.seq(envoyConfig.get("static_resources").get("listeners"))
        .map(listener -> listener
            .get("address")
            .get("socket_address"))
        .cast(ObjectNode.class)
        .filter(socketAddress -> socketAddress.has("port_value")
            && socketAddress.get("port_value").asText().startsWith("$"))
        .forEach(socketAddress -> socketAddress.put("port_value",
            Optional.ofNullable(
                LISTEN_SOCKET_ADDRESS_PORT_MAPPING.get(socketAddress
                .get("port_value")
                .asText()
                .substring(1)))
            .orElseThrow(() -> new IllegalArgumentException(
                "Can not replace value " + socketAddress.get("port_value").asText()
                .substring(1) + " of field"
                + " .static_resources.listeners[].address.socket_address.port_value"
                + " in Envoy configuration"))));

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
        .filter(socketAddress -> socketAddress.has("port_value")
            && socketAddress.get("port_value").asText().startsWith("$"))
        .forEach(socketAddress -> socketAddress.put("port_value",
            Optional.ofNullable(CLUSTER_SOCKET_ADDRESS_PORT_MAPPING.get(socketAddress
                .get("port_value")
                .asText()
                .substring(1)))
            .orElseThrow(() -> new IllegalArgumentException(
                "Can not replace value " + socketAddress.get("port_value").asText()
                .substring(1) + " of field"
                + " .static_resources.clusters[].load_assignment.endpoints[].lb_endpoints[]"
                  + ".address.socket_address.port_value"
                + " in Envoy configuration"))));

    setupPgBouncer(stackGresCluster, envoyConfig);

    setupSsl(stackGresCluster, envoyConfig);

    setupBabelfish(context, envoyConfig);

    final Map<String, String> data;
    try {
      data = ImmutableMap.of("envoy.json",
          objectMapper.writeValueAsString(envoyConfig));
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

  private void setupPgBouncer(final StackGresCluster stackGresCluster,
      final ObjectNode envoyConfig) {
    boolean disablePgBouncer = Optional
        .ofNullable(stackGresCluster.getSpec())
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getDisableConnectionPooling)
        .orElse(false);
    final String postgresEntryClusterName;
    if (disablePgBouncer) {
      Seq.seq(envoyConfig.get("static_resources").get("clusters"))
          .zipWithIndex()
          .filter(cluster -> cluster.v1.has("name")
              && cluster.v1.get("name").asText().equals("postgres_cluster_pool"))
          .findFirst()
          .ifPresent(cluster -> ((ArrayNode) envoyConfig.get("static_resources").get("clusters"))
              .remove(cluster.v2.intValue()));

      postgresEntryClusterName = "postgres_cluster";
    } else {
      postgresEntryClusterName = "postgres_cluster_pool";
    }
    Seq.seq(envoyConfig.get("static_resources").get("listeners"))
        .flatMap(listener -> Seq.seq(listener.get("filter_chains").elements()))
        .flatMap(filterChain -> Seq.seq(filterChain.get("filters").elements()))
        .map(filter -> filter
            .get("typed_config"))
        .cast(ObjectNode.class)
        .filter(typedConfig -> typedConfig.has("cluster")
            && typedConfig.get("cluster").asText().equals("$postgres_entry_cluster_name"))
        .forEach(typedConfig -> typedConfig.put("cluster", postgresEntryClusterName));
  }

  private void setupSsl(final StackGresCluster stackGresCluster, final ObjectNode envoyConfig) {
    boolean enableSsl = Optional
        .ofNullable(stackGresCluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false);
    Seq.seq(envoyConfig.get("static_resources").get("listeners"))
        .flatMap(listener -> Seq.seq(listener.get("filter_chains").elements()))
        .flatMap(filterChain -> Seq.seq(filterChain.get("filters").elements()))
        .map(filter -> filter
            .get("typed_config"))
        .cast(ObjectNode.class)
        .filter(typedConfig -> typedConfig.has("terminate_ssl")
            && typedConfig.get("terminate_ssl").asText().startsWith("$"))
        .forEach(typedConfig -> typedConfig.put("terminate_ssl", enableSsl));
    if (!enableSsl) {
      Seq.seq(envoyConfig.get("static_resources").get("listeners"))
          .flatMap(listener -> Seq.seq(listener.get("filter_chains").elements()))
          .filter(filterChain -> filterChain.has("transport_socket")
              && filterChain.get("transport_socket").has("name")
              && filterChain.get("transport_socket").get("name").asText()
              .equals("starttls"))
          .cast(ObjectNode.class)
          .forEach(filterChain -> filterChain.remove("transport_socket"));
    }
  }

  private void setupBabelfish(StackGresClusterContext context, final ObjectNode envoyConfig) {
    boolean disableBabelfish =
        getPostgresFlavorComponent(context.getCluster()) != StackGresComponent.BABELFISH;
    if (disableBabelfish) {
      Seq.seq(envoyConfig.get("static_resources").get("clusters"))
          .zipWithIndex()
          .filter(cluster -> cluster.v1.has("name")
              && cluster.v1.get("name").asText().equals("babelfish_cluster"))
          .findFirst()
          .ifPresent(cluster -> ((ArrayNode) envoyConfig.get("static_resources").get("clusters"))
              .remove(cluster.v2.intValue()));
    }
  }

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context) {
    return Seq.seq(containerUserOverrideMounts.getVolumeMounts(context))
        .append(Optional.ofNullable(context.getClusterContext().getSource().getSpec())
            .map(StackGresClusterSpec::getPostgres)
            .map(StackGresClusterPostgres::getSsl)
            .filter(ssl -> Optional.ofNullable(ssl.getEnabled()).orElse(false))
            .stream()
            .flatMap(ssl -> Seq.of(
                new VolumeMountBuilder()
                .withName("ssl")
                .withMountPath("/etc/ssl/server.crt")
                .withSubPath(ssl.getCertificateSecretKeySelector().getKey())
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName("ssl")
                .withMountPath("/etc/ssl/server.key")
                .withSubPath(ssl.getPrivateKeySecretKeySelector().getKey())
                .withReadOnly(true)
                .build())))
        .toList();
  }

}
