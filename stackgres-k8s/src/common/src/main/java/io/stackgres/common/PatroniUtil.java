/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;
import java.util.Objects;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;

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

  String SUFFIX = "-patroni";
  String READ_WRITE_SERVICE = "-primary";
  String READ_ONLY_SERVICE = "-replicas";
  String FAILOVER_SERVICE = "-failover";
  String REST_SERVICE = "-rest";
  String CONFIG_SERVICE = "-config";
  int POSTGRES_SERVICE_PORT = 5432;
  int REPLICATION_SERVICE_PORT = 5433;
  int BABELFISH_SERVICE_PORT = 1433;

  static String name(StackGresCluster cluster) {
    String name = cluster.getMetadata().getName();
    return name(name);
  }

  static String name(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName);
  }

  static String readWriteName(StackGresCluster cluster) {
    String name = cluster.getMetadata().getName();
    return readWriteName(name);
  }

  static String readWriteName(@NotNull String clusterName) {
    return ResourceUtil.nameIsValidService(clusterName + READ_WRITE_SERVICE);
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
    return name(name + REST_SERVICE);
  }

  static String configName(StackGresCluster cluster) {
    return ResourceUtil.nameIsValidDnsSubdomain(
        clusterScope(cluster) + CONFIG_SERVICE);
  }

  static String clusterScope(StackGresCluster cluster) {
    return cluster.getMetadata().getName();
  }

  /**
   * Return true when labels match a patroni primary pod, false otherwise.
   */
  static boolean isPrimary(Map<String, String> labels) {
    return Objects.equals(labels.get(ROLE_KEY), PRIMARY_ROLE);
  }

}
