/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.stackgres.common.ObjectMapperProvider;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ClusterRunningContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import org.jooq.lambda.Seq;

@Singleton
@Sidecar(AbstractEnvoy.NAME)
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@RunningContainer(ClusterRunningContainer.ENVOY)
public class Envoy extends AbstractEnvoy {

  private final ObjectMapper objectMapper;
  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;

  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider,
      ObjectMapperProvider objectMapperProvider,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      @ProviderName(CONTAINER_USER_OVERRIDE)
      VolumeMountsProvider<ContainerContext> containerUserOverrideMounts) {
    super(yamlMapperProvider, labelFactory);
    this.objectMapper = objectMapperProvider.objectMapper();
    this.containerUserOverrideMounts = containerUserOverrideMounts;
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
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_ENTRY_PORT).build(),
            new ContainerPortBuilder()
                .withProtocol("TCP")
                .withContainerPort(EnvoyUtil.PG_REPL_ENTRY_PORT).build())
        .withCommand("/usr/local/bin/envoy")
        .withArgs(Seq.of("-c", "/etc/envoy/default_envoy.json")
            .append(Seq.of(ENVOY_LOGGER.isTraceEnabled())
                .filter(traceEnabled -> traceEnabled)
                .map(traceEnabled -> ImmutableList.of("-l", "debug"))
                .flatMap(List::stream))
            .toArray(String[]::new));
    return container.build();
  }

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
    return envoyConfPath;
  }

  @Override
  protected HasMetadata buildSource(StackGresClusterContext context) {
    final StackGresCluster stackGresCluster = context.getSource();
    boolean disablePgBouncer = Optional
        .ofNullable(stackGresCluster.getSpec())
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getDisableConnectionPooling)
        .orElse(false);
    final String envoyConfPath = getEnvoyConfigPath(stackGresCluster, disablePgBouncer);

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
      data = ImmutableMap.of("default_envoy.json",
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

  @Override
  public String getImageName() {
    return StackGresComponent.ENVOY.findLatestImageName();
  }
}
