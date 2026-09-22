/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ConfigContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSamplingStatus;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresDbOpsContext extends GenerationContext<StackGresDbOps>, ClusterContext, ConfigContext {

  StackGresConfig getConfig();

  Optional<StackGresCluster> getFoundCluster();

  Optional<StackGresInstanceProfile> getFoundProfile();

  Optional<StackGresDbOpsSamplingStatus> getSamplingStatus();

  Optional<List<Pod>> getFoundClusterPods();

  Optional<List<PatroniMember>> getFoundClusterPatroniMembers();

  @Override
  @Value.Lazy
  default StackGresCluster getCluster() {
    return getFoundCluster()
        .orElseThrow(() -> new IllegalStateException(
            "SGDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " has no SGCluster " + getSource().getSpec().getSgCluster()
                + " in its context since the operation is already completed"));
  }

  @Value.Lazy
  default List<Pod> getClusterPods() {
    return getFoundClusterPods()
        .orElseThrow(() -> new IllegalStateException(
            "SGDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " has no Pods of SGCluster " + getSource().getSpec().getSgCluster()
                + " in its context since the operation is already completed"));
  }

  @Value.Lazy
  default List<PatroniMember> getClusterPatroniMembers() {
    return getFoundClusterPatroniMembers()
        .orElseThrow(() -> new IllegalStateException(
            "SGDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " has no Patroni members of SGCluster "
                + getSource().getSpec().getSgCluster()
                + " in its context since the operation is already completed"));
  }

  @Value.Lazy
  default StackGresInstanceProfile getProfile() {
    return getFoundProfile()
        .orElseThrow(() -> new IllegalStateException(
            "SGDbOps " + getSource().getMetadata().getNamespace() + "."
                + getSource().getMetadata().getName()
                + " target SGCluster " + getSource().getSpec().getSgCluster()
                + " has no SGInstanceProfile in its context: "
                + getFoundCluster()
                    .map(StackGresCluster::getSpec)
                    .map(StackGresClusterSpec::getSgInstanceProfile)
                    .orElse("<unknown>")));
  }

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  public static class Builder extends ImmutableStackGresDbOpsContext.Builder {
  }

  public static Builder builder() {
    return new Builder();
  }

}
