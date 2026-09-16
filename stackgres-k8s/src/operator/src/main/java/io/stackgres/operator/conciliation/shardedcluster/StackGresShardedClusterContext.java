/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static io.stackgres.operator.common.CryptoUtil.generatePassword;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ShardedClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;
import org.jooq.lambda.tuple.Tuple2;

@Value.Immutable
public interface StackGresShardedClusterContext
    extends GenerationContext<StackGresShardedCluster>, ShardedClusterContext {

  StackGresConfig getConfig();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  @Override
  default StackGresShardedCluster getShardedCluster() {
    return getSource();
  }

  StackGresCluster getCoordinator();

  /**
   * The coordinator SGCluster as found in the Kubernetes cluster (with its status), while
   * {@link #getCoordinator()} is the one generated from the SGShardedCluster. Empty when the
   * coordinator SGCluster has not been created yet.
   */
  Optional<StackGresCluster> getFoundCoordinatorCluster();

  Optional<StackGresInstanceProfile> getCoordinatorProfile();

  Optional<StackGresPostgresConfig> getCoordinatorPostgresConfig();

  Optional<StackGresPoolingConfig> getCoordinatorPoolingConfig();

  List<StackGresCluster> getWorkers();

  List<Tuple2<Integer, Optional<StackGresInstanceProfile>>> getWorkersProfiles();

  List<Tuple2<Integer, Optional<StackGresPostgresConfig>>> getWorkersPostgresConfigs();

  List<Tuple2<Integer, Optional<StackGresPoolingConfig>>> getWorkersPoolingConfigs();

  List<StackGresCluster> getQueryRouters();

  List<Tuple2<Integer, Optional<StackGresInstanceProfile>>> getQueryRoutersProfiles();

  List<Tuple2<Integer, Optional<StackGresPostgresConfig>>> getQueryRoutersPostgresConfigs();

  List<Tuple2<Integer, Optional<StackGresPoolingConfig>>> getQueryRoutersPoolingConfigs();

  Optional<Endpoints> getCoordinatorPrimaryEndpoints();

  List<Endpoints> getWorkersPrimaryEndpoints();

  List<Endpoints> getQueryRoutersPrimaryEndpoints();

  Optional<Secret> getDatabaseSecret();

  Set<String> getClusterBackupNamespaces();

  Optional<String> getSuperuserUsername();

  Optional<String> getSuperuserPassword();

  @Value.Derived
  default String getGeneratedSuperuserPassword() {
    return generatePassword();
  }

  Optional<String> getReplicationUsername();

  Optional<String> getReplicationPassword();

  @Value.Derived
  default String getGeneratedReplicationPassword() {
    return generatePassword();
  }

  Optional<String> getAuthenticatorUsername();

  Optional<String> getAuthenticatorPassword();

  Optional<String> getUserPasswordForBinding();

  @Value.Derived
  default String getGeneratedAuthenticatorPassword() {
    return generatePassword();
  }

  Optional<String> getPatroniRestApiPassword();

  @Value.Derived
  default String getGeneratedPatroniRestApiPassword() {
    return generatePassword();
  }

  @Value.Derived
  default String getGeneratedBabelfishPassword() {
    return generatePassword();
  }

  @Value.Derived
  default String getGeneratedPgBouncerAdminPassword() {
    return generatePassword();
  }

  @Value.Derived
  default String getGeneratedPgBouncerStatsPassword() {
    return generatePassword();
  }

  Optional<String> getPostgresSslCertificate();

  Optional<String> getPostgresSslPrivateKey();

  List<Tuple2<String, String>> getShardingSphereAuthorityUsers();

  Optional<StackGresShardedCluster> getReplicateCluster();

  public static class Builder extends ImmutableStackGresShardedClusterContext.Builder {
  }

  public static Builder builder() {
    return new Builder();
  }

}
