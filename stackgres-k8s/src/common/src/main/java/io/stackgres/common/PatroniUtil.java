/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;
import static io.stackgres.operatorframework.resource.ResourceUtil.getIndexPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.CustomContainer;
import io.stackgres.common.crd.CustomServicePort;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresService;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresServices;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.DistributedLogsLabelFactory;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl.PatroniCtlInstance;
import io.stackgres.common.patroni.PatroniHistoryEntry;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.slf4j.LoggerFactory;

public interface PatroniUtil {

  String LEADER_KEY = "leader";
  String INITIALIZE_KEY = "initialize";
  String CONFIG_KEY = "config";
  String ROLE_KEY = "role";
  String PRIMARY_ROLE = "master";
  String REPLICA_ROLE = "replica";
  String PROMOTED_ROLE = "promoted";
  String DEMOTED_ROLE = "demoted";
  String UNINITIALIZED_ROLE = "uninitialized";
  String STANDBY_LEADER_ROLE = "standby_leader";
  String SYNC_STANDBY_ROLE = "sync_standby";

  String NOLOADBALANCE_TAG = "noloadbalance";
  String NOFAILOVER_TAG = "nofailover";
  String TRUE_TAG_VALUE = "true";
  String FALSE_TAG_VALUE = "false";

  String SUFFIX = "-patroni";
  String DEPRECATED_READ_WRITE_SERVICE = "-primary";
  String READ_ONLY_SERVICE = "-replicas";
  String REST_SERVICE = "-rest";
  String CONFIG_SERVICE = "-config";
  String SYNC_SERVICE = "-sync";
  String FAILOVER_SERVICE = "-failover";
  int POSTGRES_SERVICE_PORT = 5432;
  int REPLICATION_SERVICE_PORT = 5433;
  int BABELFISH_SERVICE_PORT = 1433;

  List<String> PATRONI_BLOCKLIST_CONFIG_KEYS = List.of(
      "scope",
      "name",
      "namespace",
      "log",
      "bootstrap",
      "postgresql",
      "restapi",
      "ctl",
      "tags");

  static String clusterScope(StackGresCluster cluster) {
    return Optional
        .ofNullable(cluster.getSpec().getConfigurations().getPatroni())
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(StackGresClusterPatroniConfig::getScope)
        .orElse(cluster.getMetadata().getName());
  }

  static String clusterScope(StackGresDistributedLogs cluster) {
    return cluster.getMetadata().getName();
  }

  static String readWriteName(CustomResource<?, ?> cluster) {
    return ResourceUtil.nameIsValidService(baseNameFor(cluster));
  }

  static String readWriteNameForDistributedLogs(String name) {
    return ResourceUtil.nameIsValidService(name);
  }

  static String deprecatedReadWriteName(StackGresCluster cluster) {
    String name = cluster.getMetadata().getName();
    return deprecatedReadWriteName(name);
  }

  static String deprecatedReadWriteName(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName + DEPRECATED_READ_WRITE_SERVICE);
  }

  static String readOnlyName(StackGresCluster cluster) {
    String name = cluster.getMetadata().getName();
    return readOnlyName(name);
  }

  static String readOnlyName(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName + READ_ONLY_SERVICE);
  }

  static String roleName(StackGresCluster cluster) {
    return roleName(cluster.getMetadata().getName());
  }

  static String roleName(String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName + SUFFIX);
  }

  static String restName(StackGresCluster cluster) {
    return restName(cluster.getMetadata().getName());
  }

  static String restName(String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName + REST_SERVICE);
  }

  static String configName(CustomResource<?, ?> cluster) {
    return ResourceUtil.nameIsValidDnsSubdomain(baseNameFor(cluster) + CONFIG_SERVICE);
  }

  static String failoverName(CustomResource<?, ?> cluster) {
    return ResourceUtil.nameIsValidDnsSubdomain(baseNameFor(cluster) + FAILOVER_SERVICE);
  }

  static String syncName(CustomResource<?, ?> cluster) {
    return ResourceUtil.nameIsValidDnsSubdomain(baseNameFor(cluster) + SYNC_SERVICE);
  }

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  private static String baseNameFor(CustomResource<?, ?> resource) {
    if (resource instanceof StackGresCluster cluster) {
      return baseName(cluster);
    } else if (resource instanceof StackGresDistributedLogs cluster) {
      return baseName(cluster);
    }
    throw new IllegalArgumentException("Can not deternime base name for custom resource of kind "
        + resource.getKind());
  }

  /**
   * The patroni base name used to construct the read-write endpoints name and other endpoints names
   *  is the SGCluster name unless patroni scope is provided.
   * When patroni scope is provided the patroni base name is the scope unless the patroni citus
   *  group is provided.
   * When the patroni scope and the patroni citus group are provided the patroni base name is the
   *  scope concatenated with a dash (`-`) and the group.
   */
  private static String baseName(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(StackGresClusterPatroniConfig::getScope)
        .map(scope -> Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getConfigurations)
            .map(StackGresClusterConfigurations::getPatroni)
            .map(StackGresClusterPatroni::getInitialConfig)
            .flatMap(StackGresClusterPatroniConfig::getCitusGroup)
            .map(group -> scope + "-" + group)
            .orElse(scope))
        .orElse(cluster.getMetadata().getName());
  }

  private static String baseName(StackGresDistributedLogs cluster) {
    return cluster.getMetadata().getName();
  }

  /**
   * Return true when Pod is the primary, false otherwise.
   */
  static Boolean isPrimary(final String podName, final PatroniCtlInstance patroniCtl) {
    return patroniCtl.list()
        .stream()
        .anyMatch(member -> member.getMember().equals(podName)
            && member.isPrimary());
  }

  static boolean isBootstrapped(final PatroniCtlInstance patroniCtl) {
    return patroniCtl.list().stream().anyMatch(member -> member.getTimeline() != null);
  }

  static boolean isStandbyCluster(PatroniCtlInstance patroniCtl) {
    return patroniCtl.showConfig().getStandbyCluster() != null;
  }

  static int getLatestPrimaryIndexFromPatroni(PatroniCtlInstance patroniCtl) {
    try {
      return Seq.seq(patroniCtl.history())
          .findLast()
          .map(PatroniHistoryEntry::getNewLeader)
          .map(getIndexPattern()::matcher)
          .filter(Matcher::find).filter(matcher -> matcher.group(1) != null)
          .map(matcher -> matcher.group(1)).map(Integer::parseInt).orElse(0);
    } catch (RuntimeException ex) {
      LoggerFactory.getLogger(PatroniUtil.class)
          .warn("Unable to parse patroni history to indentify previous primary instance", ex);
      return 0;
    }
  }

  static String secretName(final String clusterName) {
    return ResourceUtil.resourceName(clusterName);
  }

  static <T extends CustomResource<?, ?>> String getInitialConfig(
      T customResource,
      LabelFactoryForCluster<T> labelFactory,
      YAMLMapper yamlMapper,
      ObjectMapper objectMapper) {
    if (customResource instanceof StackGresCluster cluster
        && labelFactory instanceof ClusterLabelFactory clusterLabelFactory) {
      return getInitialConfig(cluster, clusterLabelFactory, yamlMapper, objectMapper);
    }
    if (customResource instanceof StackGresDistributedLogs cluster
        && labelFactory instanceof DistributedLogsLabelFactory clusterLabelFactory) {
      return getInitialConfig(cluster, clusterLabelFactory, yamlMapper, objectMapper);
    }
    throw new IllegalArgumentException(customResource.getClass().getSimpleName() + " is not supporter"
        + " or do not match with " + labelFactory.getClass().getSimpleName());
  }

  static String getInitialConfig(
      StackGresCluster cluster,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      YAMLMapper yamlMapper,
      ObjectMapper objectMapper) {
    return getInitialConfig(
        cluster, labelFactory, yamlMapper, objectMapper,
        Optional.ofNullable(cluster.getSpec().getConfigurations().getPatroni())
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(Unchecked.function(yamlMapper::valueToTree))
        .map(ObjectNode.class::cast)
        .map(config -> {
          PatroniUtil.PATRONI_BLOCKLIST_CONFIG_KEYS.forEach(config::remove);
          return config;
        })
        .filter(Predicate.not(ObjectNode::isEmpty)),
        PatroniUtil.clusterScope(cluster),
        Optional
        .ofNullable(cluster.getSpec().getConfigurations())
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(StackGresClusterPatroniConfig::isPatroniOnKubernetes)
        .orElse(true));
  }

  static String getInitialConfig(
      StackGresDistributedLogs cluster,
      LabelFactoryForCluster<StackGresDistributedLogs> labelFactory,
      YAMLMapper yamlMapper,
      ObjectMapper objectMapper) {
    return getInitialConfig(
        cluster, labelFactory, yamlMapper, objectMapper,
        Optional.empty(),
        PatroniUtil.clusterScope(cluster),
        true);
  }

  static <T extends CustomResource<?, ?>> String getInitialConfig(T cluster,
      LabelFactoryForCluster<T> labelFactory,
      YAMLMapper yamlMapper,
      ObjectMapper objectMapper,
      Optional<ObjectNode> initialConfig,
      String scope,
      boolean isPatroniOnKubernetes) {
    return initialConfig
        .or(() -> Optional.of(yamlMapper.createObjectNode()))
        .map(config -> {
          config.put("scope", scope);
          if (isPatroniOnKubernetes) {
            ObjectNode kubernetes = yamlMapper.createObjectNode();
            kubernetes.put("namespace", cluster.getMetadata().getNamespace());
            kubernetes.set("labels", getClusterLabelsAsJson(cluster, objectMapper, labelFactory));
            kubernetes.put("use_endpoints", true);
            kubernetes.put("scope_label", labelFactory.labelMapper().clusterScopeKey(cluster));
            kubernetes.set("ports", getPatroniEndpointPortsAsJson(cluster, objectMapper));
            config.set("kubernetes", kubernetes);
          }
          return config;
        })
        .map(Unchecked.function(yamlMapper::writeValueAsString))
        .orElse("");
  }

  private static <T extends CustomResource<?, ?>> JsonNode getClusterLabelsAsJson(
      T cluster,
      ObjectMapper objectMapper,
      LabelFactoryForCluster<T> labelFactory) {
    final Map<String, String> patroniClusterLabels = labelFactory
        .patroniClusterLabels(cluster);
    return objectMapper.valueToTree(patroniClusterLabels);
  }

  private static <T extends CustomResource<?, ?>> JsonNode getPatroniEndpointPortsAsJson(
      T cluster, ObjectMapper objectMapper) {
    List<EndpointPort> patroniEndpointPorts = getPatroniEndpointPorts(cluster);
    return objectMapper.valueToTree(patroniEndpointPorts);
  }

  static List<EndpointPort> getPatroniEndpointPorts(final CustomResource<?, ?> customResource) {
    if (customResource instanceof StackGresCluster cluster) {
      return getPatroniEndpointPorts(cluster);
    }
    if (customResource instanceof StackGresDistributedLogs cluster) {
      return getPatroniEndpointPorts(cluster);
    }
    throw new IllegalArgumentException(customResource.getClass().getSimpleName() + " is not supporter");
  }

  static List<EndpointPort> getPatroniEndpointPorts(final StackGresCluster cluster) {
    List<EndpointPort> patroniEndpointPorts = new ArrayList<>();
    patroniEndpointPorts.add(new EndpointPortBuilder()
        .withName(EnvoyUtil.POSTGRES_PORT_NAME)
        .withPort(EnvoyUtil.PG_ENTRY_PORT)
        .withProtocol("TCP")
        .build());
    patroniEndpointPorts.add(new EndpointPortBuilder()
        .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
        .withPort(EnvoyUtil.PG_REPL_ENTRY_PORT)
        .withProtocol("TCP")
        .build());
    if (getPostgresFlavorComponent(cluster) == StackGresComponent.BABELFISH) {
      patroniEndpointPorts.add(new EndpointPortBuilder()
          .withName(EnvoyUtil.BABELFISH_PORT_NAME)
          .withPort(EnvoyUtil.BF_ENTRY_PORT)
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
    return patroniEndpointPorts;
  }

  static List<EndpointPort> getPatroniEndpointPorts(final StackGresDistributedLogs cluster) {
    List<EndpointPort> patroniEndpointPorts = new ArrayList<>();
    patroniEndpointPorts.add(new EndpointPortBuilder()
        .withName(EnvoyUtil.POSTGRES_PORT_NAME)
        .withPort(EnvoyUtil.PG_ENTRY_PORT)
        .withProtocol("TCP")
        .build());
    patroniEndpointPorts.add(new EndpointPortBuilder()
        .withName(EnvoyUtil.POSTGRES_REPLICATION_PORT_NAME)
        .withPort(EnvoyUtil.PG_REPL_ENTRY_PORT)
        .withProtocol("TCP")
        .build());
    return patroniEndpointPorts;
  }

  private static EndpointPort getEndpointPortFromCustomPort(final StackGresCluster cluster,
      CustomServicePort customPort) {
    return new EndpointPortBuilder()
        .withName(StackGresPort.CUSTOM.getName(customPort.getName()))
        .withPort(getPortForCustomTargetPort(cluster, customPort.getTargetPort()))
        .withProtocol(customPort.getProtocol())
        .withAppProtocol(customPort.getAppProtocol())
        .build();
  }

  private static Integer getPortForCustomTargetPort(final StackGresCluster cluster,
      IntOrString targetPort) {
    if (targetPort.getIntVal() != null) {
      return targetPort.getIntVal();
    }
    return findContainerPortForCustomPort(cluster, targetPort)
        .orElseThrow(() -> new IllegalArgumentException(
            "Can not find any custom container with port named "
                + targetPort.getStrVal()));
  }

  private static Optional<Integer> findContainerPortForCustomPort(final StackGresCluster cluster,
      IntOrString targetPort) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getCustomContainers)
        .stream()
        .flatMap(List::stream)
        .map(CustomContainer::getPorts)
        .flatMap(List::stream)
        .filter(port -> Objects.equals(targetPort.getStrVal(), port.getName()))
        .findFirst()
        .map(ContainerPort::getContainerPort);
  }

}
