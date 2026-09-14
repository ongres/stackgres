/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

@ApplicationScoped
public class ShardedClusterWorkersInstanceProfileContextAppender {

  private final CustomResourceFinder<StackGresInstanceProfile> profileFinder;
  private final DefaultProfileFactory defaultProfileFactory;

  public ShardedClusterWorkersInstanceProfileContextAppender(
      CustomResourceFinder<StackGresInstanceProfile> profileFinder,
      DefaultProfileFactory defaultProfileFactory) {
    this.profileFinder = profileFinder;
    this.defaultProfileFactory = defaultProfileFactory;
  }

  public void appendContext(
      StackGresShardedCluster cluster,
      Builder contextBuilder,
      List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> workers,
      List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> queryRouters) {
    var workersProfiles = workers
        .stream()
        .map(worker -> findProfiles(cluster, worker,
            cluster.getSpec().getWorkers().getSgInstanceProfile()))
        .toList();
    contextBuilder.workersProfiles(workersProfiles);
    // Query routers inherit their spec from the coordinator (see
    // StackGresShardedClusterForUtil.getBaseQueryRouterCluster), so the coordinator
    // SGInstanceProfile, and not the workers one, is their default.
    var queryRoutersProfiles = queryRouters
        .stream()
        .map(queryRouter -> findProfiles(cluster, queryRouter,
            cluster.getSpec().getCoordinator().getSgInstanceProfile()))
        .toList();
    contextBuilder.queryRoutersProfiles(queryRoutersProfiles);
  }

  private Tuple2<Integer, Optional<StackGresInstanceProfile>> findProfiles(
      StackGresShardedCluster cluster,
      Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster> worker,
      String defaultInstanceProfileName) {
    final String workerInstanceProfileName = worker.v2
        .map(StackGresShardedClusterWorker::getSgInstanceProfile)
        .orElse(defaultInstanceProfileName);
    final Optional<StackGresInstanceProfile> workersProfile = profileFinder
        .findByNameAndNamespace(
            workerInstanceProfileName,
            cluster.getMetadata().getNamespace());
    if (!workerInstanceProfileName
        .equals(defaultProfileFactory.getDefaultResourceName(cluster))
        && workersProfile.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresInstanceProfile.KIND
          + " " + workerInstanceProfileName + " was not found");
    }
    return Tuple.tuple(worker.v1, workersProfile);
  }

}
