/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresPort;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.CustomContainer;
import io.stackgres.common.crd.CustomServicePort;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class PatroniConfigMap implements VolumeFactory<StackGresClusterContext> {

  public static final int PATRONI_LOG_FILE_SIZE = 256 * 1024 * 1024;

  private static final Logger PATRONI_LOGGER = LoggerFactory.getLogger("io.stackgres.patroni");

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;
  private final ObjectMapper jsonMapper;
  private final YAMLMapper yamlMapper;

  @Inject
  public PatroniConfigMap(LabelFactoryForCluster<StackGresCluster> labelFactory,
      ObjectMapper jsonMapper,
      YamlMapperProvider yamlMapperProvider) {
    this.labelFactory = labelFactory;
    this.jsonMapper = jsonMapper;
    this.yamlMapper = yamlMapperProvider.get();
  }

  public static String name(ClusterContext clusterContext) {
    return StackGresVolume.PATRONI_ENV
        .getResourceName(clusterContext.getCluster().getMetadata().getName());
  }

  public String getKubernetesPorts(final StackGresCluster cluster,
      final int pgPort, final int pgRawPort, final int babelfishPort) {
    List<EndpointPort> patroniEndpointPorts = new ArrayList<>();
    patroniEndpointPorts.add(new EndpointPortBuilder()
        .withName(EnvoyUtil.POSTGRES_PORT_NAME)
        .withPort(pgPort)
        .withProtocol("TCP")
        .build());
    patroniEndpointPorts.add(new EndpointPortBuilder()
        .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
        .withPort(pgRawPort)
        .withProtocol("TCP")
        .build());
    if (getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH) {
      patroniEndpointPorts.add(new EndpointPortBuilder()
          .withName(EnvoyUtil.BABELFISH_PORT_NAME)
          .withPort(babelfishPort)
          .withProtocol("TCP")
          .build());
    }
    Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgresServices)
        .map(StackGresClusterPostgresServices::getPrimary)
        .map(StackGresClusterPostgresService::getCustomPorts)
        .stream()
        .flatMap(List::stream)
        .map(customPort -> getEndpointPortFromCustomPort(cluster, customPort))
        .forEach(patroniEndpointPorts::add);
    try {
      return jsonMapper.writeValueAsString(patroniEndpointPorts);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
  }

  private EndpointPort getEndpointPortFromCustomPort(final StackGresCluster cluster,
      CustomServicePort customPort) {
    return new EndpointPortBuilder()
        .withName(StackGresPort.CUSTOM.getName(customPort.getName()))
        .withPort(getPortForCustomTargetPort(cluster, customPort.getTargetPort()))
        .withProtocol(customPort.getProtocol())
        .withAppProtocol(customPort.getAppProtocol())
        .build();
  }

  private Integer getPortForCustomTargetPort(final StackGresCluster cluster,
      IntOrString targetPort) {
    if (targetPort.getIntVal() != null) {
      return targetPort.getIntVal();
    }
    return findContainerPortForCustomPort(cluster, targetPort)
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not find any custom container with port named "
                + targetPort.getStrVal()));
  }

  private Optional<Integer> findContainerPortForCustomPort(final StackGresCluster cluster,
      IntOrString targetPort) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getCustomContainers)
        .stream()
        .flatMap(List::stream)
        .map(CustomContainer::getPorts)
        .flatMap(List::stream)
        .filter(port -> Objects.equals(targetPort.getStrVal(), port.getName()))
        .findFirst()
        .map(ContainerPort::getContainerPort);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build()
    );
  }

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.PATRONI_ENV.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .withDefaultMode(0444)
            .build())
        .build();
  }

  public @NotNull ConfigMap buildSource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final String pgVersion = getPostgresFlavorComponent(cluster).get(cluster).getVersion(
        cluster.getSpec().getPostgres().getVersion());

    final String patroniClusterLabelsAsJson;
    final Map<String, String> patroniClusterLabels = labelFactory
        .patroniClusterLabels(cluster);
    try {
      patroniClusterLabelsAsJson = jsonMapper.writeValueAsString(
          patroniClusterLabels);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }

    final String pgHost = "127.0.0.1"; // NOPMD
    final int pgRawPort = EnvoyUtil.PG_REPL_ENTRY_PORT;
    final int pgPort = EnvoyUtil.PG_ENTRY_PORT;
    final int babelfishPort = EnvoyUtil.BF_ENTRY_PORT;
    Map<String, String> data = new HashMap<>();
    data.put("PATRONI_CONFIG_FILE", ClusterStatefulSetPath.PATRONI_CONFIG_FILE_PATH.path());
    data.put("PATRONI_SCOPE", PatroniUtil.clusterScope(cluster));
    data.put("PATRONI_INITIAL_CONFIG", Optional
        .ofNullable(cluster.getSpec().getConfiguration().getPatroni())
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(Unchecked.function(yamlMapper::valueToTree))
        .map(ObjectNode.class::cast)
        .map(config -> {
          PatroniUtil.PATRONI_BLOCKLIST_CONFIG_KEYS.forEach(config::remove);
          return config;
        })
        .filter(Predicate.not(ObjectNode::isEmpty))
        .map(Unchecked.function(yamlMapper::writeValueAsString))
        .orElse(""));
    data.put("PATRONI_KUBERNETES_SCOPE_LABEL",
        labelFactory.labelMapper().clusterScopeKey(cluster));
    data.put("PATRONI_KUBERNETES_LABELS", patroniClusterLabelsAsJson);
    data.put("PATRONI_KUBERNETES_USE_ENDPOINTS", "true");
    data.put("PATRONI_KUBERNETES_PORTS", getKubernetesPorts(
        cluster, pgPort, pgRawPort, babelfishPort));
    data.put("PATRONI_POSTGRESQL_LISTEN", pgHost + ":" + EnvoyUtil.PG_PORT);
    data.put("PATRONI_POSTGRESQL_CONNECT_ADDRESS",
        "${PATRONI_KUBERNETES_POD_IP}:" + pgRawPort);

    data.put("PATRONI_RESTAPI_LISTEN", "0.0.0.0:" + EnvoyUtil.PATRONI_PORT);
    data.put("PATRONI_POSTGRESQL_DATA_DIR", ClusterStatefulSetPath.PG_DATA_PATH.path());
    data.put("PATRONI_POSTGRESQL_BIN_DIR", "/usr/lib/postgresql/" + pgVersion + "/bin");
    data.put("PATRONI_POSTGRES_UNIX_SOCKET_DIRECTORY", ClusterStatefulSetPath.PG_RUN_PATH.path());

    if (Optional.ofNullable(cluster.getSpec().getDistributedLogs())
        .map(StackGresClusterDistributedLogs::getDistributedLogs).isPresent()) {
      data.put("PATRONI_LOG_DIR", ClusterStatefulSetPath.PG_LOG_PATH.path());
      data.put("PATRONI_LOG_FILE_NUM", "2");
      data.put("PATRONI_LOG_FILE_SIZE", String.valueOf(PATRONI_LOG_FILE_SIZE));
    }

    if (PATRONI_LOGGER.isTraceEnabled()) {
      data.put("PATRONI_LOG_LEVEL", "DEBUG");
    }

    data.put("PATRONI_SCRIPTS",
        Optional.ofNullable(
            cluster.getSpec().getInitData())
            .map(StackGresClusterInitData::getScripts)
            .map(List::size)
            .map(String::valueOf)
            .orElse("0"));

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build();
  }
}
