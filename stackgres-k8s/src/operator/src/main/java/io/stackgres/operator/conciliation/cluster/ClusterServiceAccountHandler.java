/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresCluster.class, kind = "ServiceAccount")
@ApplicationScoped
public class ClusterServiceAccountHandler implements ReconciliationHandler<StackGresCluster> {

  @Inject
  ResourceWriter<ServiceAccount> serviceAccountWriter;

  @Override
  public HasMetadata create(StackGresCluster context, HasMetadata resource) {
    var serviceAccount = safeCast(resource);
    return serviceAccountWriter.create(serviceAccount);
  }

  @Override
  public HasMetadata patch(StackGresCluster context, HasMetadata newResource,
      HasMetadata oldResource) {
    oldResource.setMetadata(newResource.getMetadata());
    return serviceAccountWriter.update(safeCast(oldResource));
  }

  @Override
  public HasMetadata replace(StackGresCluster context, HasMetadata resource) {
    return serviceAccountWriter.update(safeCast(resource));
  }

  @Override
  public void delete(StackGresCluster context, HasMetadata resource) {
    serviceAccountWriter.delete(safeCast(resource));
  }

  private static ServiceAccount safeCast(HasMetadata resource) {
    if (!(resource instanceof ServiceAccount)) {
      throw new IllegalArgumentException("Resource must be a ServiceAccount instance");
    }
    return (ServiceAccount) resource;
  }
}
