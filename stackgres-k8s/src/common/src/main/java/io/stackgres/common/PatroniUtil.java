/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static io.stackgres.operatorframework.resource.ResourceUtil.getIndexPattern;

import java.util.Optional;
import java.util.regex.Matcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface PatroniUtil {

  Logger LOGGER = LoggerFactory.getLogger(PatroniUtil.class);

  String SUFFIX = "-patroni";
  String READ_WRITE_SERVICE = "-primary";
  String READ_ONLY_SERVICE = "-replicas";
  String FAILOVER_SERVICE = "-failover";
  String CONFIG_SERVICE = "-config";
  int POSTGRES_SERVICE_PORT = 5432;
  int REPLICATION_SERVICE_PORT = 5433;
  int BABELFISH_SERVICE_PORT = 1433;

  static String name(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName);
  }

  static String readWriteName(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName + READ_WRITE_SERVICE);
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

  static String configName(ClusterContext context) {
    return configName(context.getCluster());
  }

  static String configName(StackGresCluster cluster) {
    return ResourceUtil.nameIsValidDnsSubdomain(
        cluster.getMetadata().getName() + CONFIG_SERVICE);
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
      LOGGER.warn("Unable to parse patroni history to indentify previous primary instance", ex);
      return 0;
    }
  }

}
