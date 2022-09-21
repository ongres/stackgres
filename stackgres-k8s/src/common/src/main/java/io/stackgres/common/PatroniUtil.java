/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.operatorframework.resource.ResourceUtil.getIndexPattern;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;
import org.slf4j.LoggerFactory;

public interface PatroniUtil {

  String LEADER_KEY = "leader";
  String INITIALIZE_KEY = "initialize";
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
  String FAILOVER_SERVICE = "-failover";
  String REST_SERVICE = "-rest";
  String CONFIG_SERVICE = "-config";
  int POSTGRES_SERVICE_PORT = 5432;
  int REPLICATION_SERVICE_PORT = 5433;
  int BABELFISH_SERVICE_PORT = 1433;

  static String readWriteName(StackGresCluster cluster) {
    String name = cluster.getMetadata().getName();
    return readWriteName(name);
  }

  static String readWriteName(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName);
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
    String name = cluster.getMetadata().getName();
    return readWriteName(name + REST_SERVICE);
  }

  static String configName(ClusterContext context) {
    return configName(context.getCluster());
  }

  static String configName(CustomResource<?, ?> cluster) {
    return ResourceUtil.nameIsValidDnsSubdomain(
        clusterScope(cluster) + CONFIG_SERVICE);
  }

  static String clusterScope(CustomResource<?, ?> cluster) {
    return cluster.getMetadata().getName();
  }

  /**
   * Return true when labels match a patroni primary pod, false otherwise.
   */
  static boolean isPrimary(Map<String, String> labels) {
    return Objects.equals(labels.get(ROLE_KEY), PRIMARY_ROLE);
  }

  static int getLatestPrimaryIndexFromPatroni(Optional<Endpoints> patroniConfigEndpoints,
      ObjectMapper objectMapper) {
    try {
      return patroniConfigEndpoints.map(Endpoints::getMetadata).map(ObjectMeta::getAnnotations)
          .filter(annotations -> annotations.containsKey("history"))
          .map(annotations -> annotations.get("history"))
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
