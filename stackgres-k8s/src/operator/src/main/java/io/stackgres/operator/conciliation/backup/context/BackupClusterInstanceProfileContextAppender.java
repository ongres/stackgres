/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup.context;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BackupClusterInstanceProfileContextAppender {

  private final CustomResourceFinder<StackGresProfile> profileFinder;

  public BackupClusterInstanceProfileContextAppender(
      CustomResourceFinder<StackGresProfile> profileFinder) {
    this.profileFinder = profileFinder;
  }

  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final Optional<StackGresProfile> foundProfile = profileFinder
        .findByNameAndNamespace(
            cluster.getSpec().getSgInstanceProfile(),
            cluster.getMetadata().getNamespace());
    if (foundProfile.isEmpty()) {
      throw new IllegalArgumentException(
          StackGresProfile.KIND + " " + cluster.getSpec().getSgInstanceProfile() + " was not found");
    }
    contextBuilder.foundProfile(foundProfile);
  }

}
