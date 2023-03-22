/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniCredentials;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniInitialConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterUserSecretKeyRef;
import io.stackgres.common.crd.sgcluster.StackGresClusterUsersCredentials;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.patroni.StackGresPasswordKeys;

public interface StackGresShardedClusterUtil {

  static StackGresCluster getCoordinatorCluster(StackGresShardedCluster cluster) {
    final StackGresClusterSpec spec =
        new StackGresClusterSpecBuilder(cluster.getSpec().getCoordinator())
        .build();
    setClusterSpecFromShardedCluster(cluster, spec, 0);
    StackGresCluster coordinatorCluster = new StackGresCluster();
    coordinatorCluster.setMetadata(new ObjectMeta());
    coordinatorCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    coordinatorCluster.getMetadata().setName(cluster.getMetadata().getName() + "-coord");
    coordinatorCluster.setSpec(spec);
    return coordinatorCluster;
  }

  static StackGresCluster getShardsCluster(StackGresShardedCluster cluster, int index) {
    final StackGresClusterSpec spec =
        new StackGresClusterSpecBuilder(cluster.getSpec().getShards())
        .build();
    setClusterSpecFromShardedCluster(cluster, spec, index + 1);
    spec.setInstances(cluster.getSpec().getShards().getInstancesPerCluster());
    StackGresCluster shardsCluster = new StackGresCluster();
    shardsCluster.setMetadata(new ObjectMeta());
    shardsCluster.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    shardsCluster.getMetadata().setName(cluster.getMetadata().getName() + "-shard" + index);
    shardsCluster.setSpec(spec);
    return shardsCluster;
  }

  private static void setClusterSpecFromShardedCluster(
      StackGresShardedCluster cluster, final StackGresClusterSpec spec, int index) {
    spec.setPostgres(
        new StackGresClusterPostgresBuilder(cluster.getSpec().getPostgres())
        .withExtensions(Optional.ofNullable(cluster.getStatus())
            .map(StackGresShardedClusterStatus::getToInstallPostgresExtensions)
            .stream()
            .flatMap(List::stream)
            .map(extension -> new StackGresClusterExtensionBuilder()
                .withName(extension.getName())
                .withPublisher(extension.getPublisher())
                .withRepository(extension.getRepository())
                .withVersion(extension.getVersion())
                .build())
            .toList())
        .build());
    spec.getConfiguration().setCredentials(new StackGresClusterCredentials());
    spec.getConfiguration().getCredentials()
        .setPatroni(new StackGresClusterPatroniCredentials());
    spec.getConfiguration().getCredentials().getPatroni()
        .setRestApiPassword(new SecretKeySelector(
            StackGresPasswordKeys.RESTAPI_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfiguration().getCredentials()
        .setUsers(new StackGresClusterUsersCredentials());
    spec.getConfiguration().getCredentials().getUsers()
        .setSuperuser(new StackGresClusterUserSecretKeyRef());
    spec.getConfiguration().getCredentials().getUsers().getSuperuser()
        .setPassword(new SecretKeySelector(
            StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfiguration().getCredentials().getUsers()
        .setReplication(new StackGresClusterUserSecretKeyRef());
    spec.getConfiguration().getCredentials().getUsers().getReplication()
        .setPassword(new SecretKeySelector(
            StackGresPasswordKeys.REPLICATION_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfiguration().getCredentials().getUsers()
        .setAuthenticator(new StackGresClusterUserSecretKeyRef());
    spec.getConfiguration().getCredentials().getUsers().getAuthenticator()
        .setPassword(new SecretKeySelector(
            StackGresPasswordKeys.AUTHENTICATOR_PASSWORD_KEY,
            cluster.getMetadata().getName()));
    spec.getConfiguration().setPatroni(new StackGresClusterPatroni());
    spec.getConfiguration().getPatroni()
        .setInitialConfig(new StackGresClusterPatroniInitialConfig());
    spec.getConfiguration().getPatroni().getInitialConfig()
        .put("scope", cluster.getMetadata().getName());
    var citus = new HashMap<String, Object>(2);
    citus.put("database", cluster.getSpec().getDatabase());
    citus.put("group", index);
    spec.getConfiguration().getPatroni().getInitialConfig()
        .put("citus", citus);
    spec.setMetadata(new StackGresClusterSpecMetadata());
    spec.getMetadata().setLabels(new StackGresClusterSpecLabels());
    spec.getMetadata().getLabels().setClusterPods(Map.of("citus-group", String.valueOf(index)));
    spec.getMetadata().getLabels().setServices(Map.of("citus-group", String.valueOf(index)));
    spec.setPrometheusAutobind(cluster.getSpec().getPrometheusAutobind());
    spec.setNonProductionOptions(cluster.getSpec().getNonProductionOptions());
    if (cluster.getStatus() != null) {
      spec.setToInstallPostgresExtensions(cluster.getStatus().getToInstallPostgresExtensions());
    }
  }

}
