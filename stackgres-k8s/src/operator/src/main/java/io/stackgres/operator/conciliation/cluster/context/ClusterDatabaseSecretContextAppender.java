/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterDatabaseSecretContextAppender
    extends ContextAppender<StackGresCluster, StackGresClusterContext.Builder> {

  private final ResourceFinder<Secret> secretFinder;

  public ClusterDatabaseSecretContextAppender(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Optional<Secret> databaseSecret =
        secretFinder.findByNameAndNamespace(
            cluster.getMetadata().getName(),
            cluster.getMetadata().getNamespace());
    contextBuilder
        .databaseSecret(databaseSecret);
  }

}
