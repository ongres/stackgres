/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.Optional;

import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresDbOpsContext extends GenerationContext<StackGresDbOps>, ClusterContext {

  Optional<StackGresCluster> getFoundCluster();

  Optional<StackGresProfile> getFoundProfile();

  @Override
  @Value.Lazy
  default StackGresCluster getCluster() {
    return getFoundCluster()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " have a non existent SGCluster "
                + getSource().getSpec().getSgCluster()));
  }

  @Value.Lazy
  default StackGresProfile getProfile() {
    return getFoundProfile()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target SGCluster " + getSource().getSpec().getSgCluster()
                + " with a non existent SGInstanceProfile "
                + getFoundCluster()
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getResourceProfile)
                    .orElse("<unknown>")));
  }

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

}
