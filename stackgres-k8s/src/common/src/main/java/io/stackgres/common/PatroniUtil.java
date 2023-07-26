/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.operatorframework.resource.ResourceUtil.getIndexPattern;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniInitialConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;
import org.slf4j.LoggerFactory;

public interface PatroniUtil {

  String LEADER_KEY = "leader";
  String INITIALIZE_KEY = "initialize";
  String CONFIG_KEY = "config";
  String HISTORY_KEY = "history";
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
      "consul",
      "etcd",
      "etcdv3",
      "zookeper",
      "exhibitor",
      "kubernetes",
      "raft",
      "postgresql",
      "restapi",
      "ctl",
      "tags");

  static String clusterScope(StackGresCluster cluster) {
    return Optional
        .ofNullable(cluster.getSpec().getConfiguration().getPatroni())
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(patroniConfig -> patroniConfig.getScope())
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
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(StackGresClusterPatroniInitialConfig::getScope)
        .map(scope -> Optional.of(cluster)
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getConfiguration)
            .map(StackGresClusterConfiguration::getPatroni)
            .map(StackGresClusterPatroni::getInitialConfig)
            .flatMap(StackGresClusterPatroniInitialConfig::getCitusGroup)
            .map(group -> scope + "-" + group)
            .orElse(scope))
        .orElse(cluster.getMetadata().getName());
  }

  private static String baseName(StackGresDistributedLogs cluster) {
    return cluster.getMetadata().getName();
  }

  /**
   * Return true when labels match a patroni primary pod, false otherwise.
   */
  static boolean isPrimary(Map<String, String> labels) {
    return Objects.equals(labels.get(ROLE_KEY), PRIMARY_ROLE);
  }

  static Boolean isPrimary(final String podName, final Optional<Endpoints> patroniEndpoints) {
    return patroniEndpoints.map(Endpoints::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(LEADER_KEY))
        .map(podName::equals).orElse(false);
  }

  static boolean isBootstrapped(final Optional<Endpoints> patroniConfigEndpoints) {
    return patroniConfigEndpoints.map(Endpoints::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(INITIALIZE_KEY))
        .filter(Predicate.not(String::isEmpty))
        .isPresent();
  }

  static boolean isStandbyCluster(Optional<Endpoints> patroniConfigEndpoints,
      ObjectMapper objectMapper) {
    return patroniConfigEndpoints
    .map(Endpoints::getMetadata)
    .map(ObjectMeta::getAnnotations)
    .map(annotations -> annotations.get(PatroniUtil.CONFIG_KEY))
    .map(Unchecked.function(config -> objectMapper.readValue(config, PatroniConfig.class)))
    .map(PatroniConfig::getStandbyCluster)
    .isPresent();
  }

  static int getLatestPrimaryIndexFromPatroni(Optional<Endpoints> patroniConfigEndpoints,
      ObjectMapper objectMapper) {
    try {
      return patroniConfigEndpoints.map(Endpoints::getMetadata).map(ObjectMeta::getAnnotations)
          .filter(annotations -> annotations.containsKey(HISTORY_KEY))
          .map(annotations -> annotations.get(HISTORY_KEY))
          .map(Unchecked.function(history -> objectMapper.readTree(history)))
          .filter(history -> history instanceof ArrayNode).map(ArrayNode.class::cast)
          .map(history -> history.get(history.size() - 1))
          .filter(lastPrimary -> lastPrimary instanceof ArrayNode).map(ArrayNode.class::cast)
          .filter(lastPrimary -> lastPrimary.size() == 5).map(lastPrimary -> lastPrimary.get(4))
          .filter(lastPrimary -> lastPrimary instanceof TextNode).map(TextNode.class::cast)
          .map(TextNode::textValue).map(getIndexPattern()::matcher)
          .filter(Matcher::find).filter(matcher -> matcher.group(1) != null)
          .map(matcher -> matcher.group(1)).map(Integer::parseInt).orElse(0);
    } catch (RuntimeException ex) {
      LoggerFactory.getLogger(PatroniUtil.class)
          .warn("Unable to parse patroni history to indentify previous primary instance", ex);
      return 0;
    }
  }

}
