/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.ClusterScheduler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ReconciliationScope(value = StackGresDbOps.class, kind = StackGresCluster.KIND)
@ApplicationScoped
public class DbOpsClusterReconciliationHandler
    implements ReconciliationHandler<StackGresDbOps> {

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(DbOpsClusterReconciliationHandler.class);

  protected final ClusterScheduler clusterScheduler;

  @Inject
  public DbOpsClusterReconciliationHandler(ClusterScheduler clusterScheduler) {
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
    return clusterScheduler.update(cluster, currentCluster -> {
      if (cluster.getMetadata().getAnnotations() != null) {
        currentCluster.getMetadata().setAnnotations(
            Seq.seq(
                Optional.ofNullable(currentCluster.getMetadata().getAnnotations())
                .map(Map::entrySet)
                .stream()
                .flatMap(Set::stream)
                .filter(annotation -> !cluster.getMetadata().getAnnotations().entrySet().contains(annotation)))
            .append(cluster.getMetadata().getAnnotations().entrySet())
            .toMap(Map.Entry::getKey, Map.Entry::getValue));
      }
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
