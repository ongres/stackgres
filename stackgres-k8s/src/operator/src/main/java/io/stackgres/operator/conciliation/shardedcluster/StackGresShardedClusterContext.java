/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static io.stackgres.operator.common.CryptoUtil.generatePassword;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.GenerationContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresShardedClusterContext
    extends GenerationContext<StackGresShardedCluster> {

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  StackGresClusterContext getCoordinator();

  StackGresPostgresConfig getCoordinatorConfig();

  List<StackGresClusterContext> getShards();

  Optional<Endpoints> getCoordinatorPrimaryEndpoints();

  List<Endpoints> getShardsPrimaryEndpoints();

  Optional<Secret> getDatabaseSecret();

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

}
