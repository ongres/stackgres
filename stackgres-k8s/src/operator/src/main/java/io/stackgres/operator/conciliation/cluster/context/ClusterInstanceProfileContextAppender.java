/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterInstanceProfileContextAppender
    extends ContextAppender<StackGresCluster, Builder> {

  private final CustomResourceFinder<StackGresProfile> profileFinder;
  private final DefaultProfileFactory defaultProfileFactory;

  public ClusterInstanceProfileContextAppender(
      CustomResourceFinder<StackGresProfile> profileFinder,
      DefaultProfileFactory defaultProfileFactory) {
    this.profileFinder = profileFinder;
    this.defaultProfileFactory = defaultProfileFactory;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Optional<StackGresProfile> profile = profileFinder
        .findByNameAndNamespace(
            cluster.getSpec().getSgInstanceProfile(),
            cluster.getMetadata().getNamespace());
    if (!cluster.getSpec().getSgInstanceProfile().equals(defaultProfileFactory.getDefaultResourceName(cluster))
        && profile.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresProfile.KIND + " " + cluster.getSpec().getSgInstanceProfile() + " was not found");
    }
    contextBuilder.profile(profile);
  }

}
