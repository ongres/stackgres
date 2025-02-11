/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterInstanceProfileContextAppender
    extends ContextAppender<StackGresCluster, StackGresClusterContext.Builder> {

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  public ClusterInstanceProfileContextAppender(CustomResourceFinder<StackGresProfile> profileFinder) {
    this.profileFinder = profileFinder;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final StackGresProfile profile = profileFinder
        .findByNameAndNamespace(
            cluster.getSpec().getSgInstanceProfile(),
            cluster.getMetadata().getNamespace())
        .orElseThrow(() -> new IllegalArgumentException(
            "SGCluster " + cluster.getMetadata().getNamespace() + "." + cluster.getMetadata().getName()
                + " have a non existent "
                + StackGresProfile.KIND + " " + cluster.getSpec().getSgInstanceProfile()));
    contextBuilder.profile(profile);
  }

}
