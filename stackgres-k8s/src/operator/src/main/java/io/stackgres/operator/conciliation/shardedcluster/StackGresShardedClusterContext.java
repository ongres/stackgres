/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.VersionInfo;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.GenerationContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresShardedClusterContext
    extends GenerationContext<StackGresShardedCluster> {

  Optional<VersionInfo> getKubernetesVersion();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  StackGresClusterContext getCoordinator();

  List<StackGresClusterContext> getShards();

  Optional<Service> getCoordinatorPrimaryService();

  Optional<Secret> getDatabaseSecret();

  Optional<String> getSuperuserUsername();

  Optional<String> getSuperuserPassword();

  Optional<String> getReplicationUsername();

  Optional<String> getReplicationPassword();

  Optional<String> getAuthenticatorUsername();

  Optional<String> getAuthenticatorPassword();

  Optional<String> getPatroniRestApiPassword();

}
