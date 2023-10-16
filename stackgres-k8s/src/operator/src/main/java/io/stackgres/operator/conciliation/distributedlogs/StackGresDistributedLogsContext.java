/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresDistributedLogsContext
    extends GenerationContext<StackGresDistributedLogs> {

  StackGresConfig getConfig();

  StackGresPostgresConfig getPostgresConfig();

  StackGresProfile getProfile();

  List<StackGresCluster> getConnectedClusters();

  Optional<Secret> getDatabaseCredentials();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

}
