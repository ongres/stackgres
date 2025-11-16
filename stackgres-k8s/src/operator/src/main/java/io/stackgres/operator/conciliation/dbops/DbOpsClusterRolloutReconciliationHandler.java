/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.ClusterScheduler;
import io.stackgres.operator.common.DbOpsUtil;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import io.stackgres.operator.conciliation.factory.dbops.DbOpsClusterRollout;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ReconciliationScope(value = StackGresDbOps.class, kind = StackGresCluster.KIND)
@ApplicationScoped
public class DbOpsClusterRolloutReconciliationHandler
    implements ReconciliationHandler<StackGresDbOps> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(DbOpsClusterRolloutReconciliationHandler.class);

  protected final ClusterScheduler clusterScheduler;

  @Inject
  public DbOpsClusterRolloutReconciliationHandler(ClusterScheduler clusterScheduler) {
    this.clusterScheduler = clusterScheduler;
  }

  private StackGresCluster safeCast(HasMetadata resource) {
    if (!(resource instanceof StackGresCluster)) {
      throw new IllegalArgumentException("Resource must be an " + StackGresCluster.KIND + " instance");
    }
    return (StackGresCluster) resource;
  }

  @Override
  public HasMetadata create(StackGresDbOps context, HasMetadata resource) {
    LOGGER.debug("Skipping creating {} {}.{}",
        HasMetadata.getKind(resource.getClass()),
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
    return resource;
  }

  @Override
  public HasMetadata patch(StackGresDbOps context, HasMetadata newResource, HasMetadata oldResource) {
    return replace(context, newResource);
  }

  @Override
  public HasMetadata replace(StackGresDbOps context, HasMetadata resource) {
    final StackGresCluster cluster = safeCast(resource);
    if (!DbOpsUtil.ROLLOUT_OPS.contains(context.getSpec().getOp())) {
      return resource;
    }
    return clusterScheduler.update(cluster, currentCluster -> {
      if (Optional.ofNullable(cluster.getSpec())
          .map(StackGresClusterSpec::getPostgres)
          .map(StackGresClusterPostgres::getVersion)
          .isPresent()) {
        currentCluster.getSpec().getPostgres().setVersion(
            cluster.getSpec().getPostgres().getVersion());
      }
      if (Optional.ofNullable(cluster.getSpec())
          .map(StackGresClusterSpec::getInstances)
          .isPresent()) {
        currentCluster.getSpec().setInstances(cluster.getSpec().getInstances());
      }
      if (cluster.getStatus() != null
          && cluster.getStatus().getDbOps() != null) {
        if (currentCluster.getStatus() == null) {
          currentCluster.setStatus(new StackGresClusterStatus());
        }
        currentCluster.getStatus().setDbOps(cluster.getStatus().getDbOps());
      } else if (Optional.ofNullable(currentCluster.getMetadata().getAnnotations())
          .map(Map::entrySet)
          .stream()
          .flatMap(Set::stream)
          .anyMatch(annotation -> StackGresContext.ROLLOUT_DBOPS_KEY.equals(annotation.getKey())
              && StackGresContext.ROLLOUT_DBOPS_KEY.equals(context.getMetadata().getName()))) {
        currentCluster.getStatus().setDbOps(null);
      }
      if (cluster.getMetadata().getAnnotations() != null
          && Optional.ofNullable(currentCluster.getMetadata().getAnnotations())
          .map(Map::entrySet)
          .stream()
          .flatMap(Set::stream)
          .noneMatch(annotation -> StackGresContext.ROLLOUT_DBOPS_KEY.equals(annotation.getKey())
              && !context.getMetadata().getName().equals(annotation.getValue()))) {
        currentCluster.getMetadata().setAnnotations(
            Seq.seq(
                Optional.ofNullable(currentCluster.getMetadata().getAnnotations())
                .map(Map::entrySet)
                .stream()
                .flatMap(Set::stream)
                .filter(annotation -> !DbOpsClusterRollout.ROLLOUT_DBOPS_KEYS.contains(annotation.getKey())))
            .append(cluster.getMetadata().getAnnotations().entrySet()
                .stream()
                .filter(annotation -> DbOpsClusterRollout.ROLLOUT_DBOPS_KEYS.contains(annotation.getKey())))
            .toMap(Map.Entry::getKey, Map.Entry::getValue));
      }
    });
  }

  @Override
  public void delete(StackGresDbOps context, HasMetadata resource) {
    LOGGER.debug("Skipping deleting {} {}.{}",
        HasMetadata.getKind(resource.getClass()),
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
  }

  @Override
  public void deleteWithOrphans(StackGresDbOps context, HasMetadata resource) {
    LOGGER.debug("Skipping deleting {} {}.{}",
        HasMetadata.getKind(resource.getClass()),
        resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
  }

}
