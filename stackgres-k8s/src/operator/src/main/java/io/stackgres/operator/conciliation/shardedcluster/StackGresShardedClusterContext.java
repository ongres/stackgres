/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

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

  Optional<String> getReplicationUsername();

  Optional<String> getReplicationPassword();

  Optional<String> getAuthenticatorUsername();

  Optional<String> getAuthenticatorPassword();

  Optional<String> getPatroniRestApiPassword();

  Optional<String> getPostgresSslCertificate();

  Optional<String> getPostgresSslPrivateKey();

}
