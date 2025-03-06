/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultInstanceProfileMutator
    extends AbstractDefaultResourceMutator<StackGresProfile, HasMetadata,
        StackGresCluster, StackGresClusterReview>
    implements ClusterMutator {

  @Inject
  public DefaultInstanceProfileMutator(
      DefaultCustomResourceFactory<StackGresProfile, HasMetadata> resourceFactory) {
    super(resourceFactory);
  }

  @Override
  protected String getTargetPropertyValue(StackGresCluster resource) {
    return resource.getSpec().getSgInstanceProfile();
  }

  @Override
  protected void setTargetProperty(StackGresCluster resource, String defaultResourceName) {
    resource.getSpec().setSgInstanceProfile(defaultResourceName);
  }

}
