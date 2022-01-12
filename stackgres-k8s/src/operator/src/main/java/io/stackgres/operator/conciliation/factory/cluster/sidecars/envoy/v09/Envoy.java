/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy.v09;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_LOCAL_OVERRIDE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ClusterRunningContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy.AbstractEnvoy;
import org.jooq.lambda.Seq;

@Singleton
@Sidecar(AbstractEnvoy.NAME)
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
@RunningContainer(ClusterRunningContainer.ENVOY_V09)
public class Envoy extends AbstractEnvoy {

  private final VolumeMountsProvider<ContainerContext> containerLocalOverrideMounts;

  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider,
               LabelFactoryForCluster<StackGresCluster> labelFactory,
               @ProviderName(CONTAINER_LOCAL_OVERRIDE)
                     VolumeMountsProvider<ContainerContext> containerLocalOverrideMounts) {
    super(yamlMapperProvider, labelFactory);
    this.containerLocalOverrideMounts = containerLocalOverrideMounts;
  }

  protected Stream<ImmutableVolumePair> buildExtraVolumes(StackGresClusterContext context) {
    return Stream.of();
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(StackGresComponent.ENVOY.get(context.getClusterContext().getCluster())
            .findLatestImageName())
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
        .withArgs("-c", "/etc/envoy/default_envoy.yaml", "-l", "debug");
    return container.build();
  }

  @Override
  protected String getEnvoyConfigPath(final StackGresCluster stackGresCluster,
      boolean disablePgBouncer) {
    final String envoyConfPath;
    if (disablePgBouncer) {
      envoyConfPath = "/envoy/envoy_nopgbouncer.yaml";
    } else {
      envoyConfPath = "/envoy/default_envoy.yaml";
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

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context) {
    return containerLocalOverrideMounts.getVolumeMounts(context);
  }

}
