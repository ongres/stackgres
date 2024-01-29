/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CoordinatorScriptsConfigMutator implements ShardedClusterMutator {

  private static final long V_1_7_0 = StackGresVersion.V_1_7.getVersionAsNumber();

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (V_1_7_0 >= version
        && Optional.ofNullable(resource.getMetadata().getOwnerReferences())
        .stream()
        .flatMap(List::stream)
        .anyMatch(owner -> owner.getApiVersion()
            .equals(HasMetadata.getApiVersion(StackGresShardedCluster.class))
            && owner.getKind()
            .equals(HasMetadata.getKind(StackGresShardedCluster.class)))) {
      Optional.of(resource.getSpec())
          .map(StackGresShardedClusterSpec::getCoordinator)
          .map(StackGresClusterSpec::getManagedSql)
          .map(StackGresClusterManagedSql::getScripts)
          .stream()
          .flatMap(List::stream)
          .filter(script -> script.getId() >= 0
            && script.getId() <= StackGresShardedClusterUtil.LAST_RESERVER_SCRIPT_ID)
          .forEach(script -> script.setId(
              script.getId() + StackGresShardedClusterUtil.LAST_RESERVER_SCRIPT_ID + 1));
    }
    fillRequiredFields(resource);
    return resource;
  }

  private void fillRequiredFields(StackGresShardedCluster resource) {
    int lastId = Optional.of(resource)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .map(StackGresClusterManagedScriptEntry::getId)
        .reduce(StackGresShardedClusterUtil.LAST_RESERVER_SCRIPT_ID,
            (last, id) -> id == null || last >= id ? last : id, (u, v) -> v);
    for (StackGresClusterManagedScriptEntry scriptEntry : Optional.of(resource)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getCoordinator)
        .map(StackGresShardedClusterCoordinator::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .orElse(List.of())) {
      if (scriptEntry.getId() == null) {
        lastId++;
        scriptEntry.setId(lastId);
      }
    }
  }

}
