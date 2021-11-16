/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresDbOps.class, kind = "ServiceAccount")
@ApplicationScoped
public class DbOpsServiceAccountHandler implements ReconciliationHandler<StackGresDbOps> {

  @Inject
  ResourceWriter<ServiceAccount> serviceAccountWriter;

  @Override
  public HasMetadata create(StackGresDbOps context, HasMetadata resource) {
    var serviceAccount = safeCast(resource);
    return serviceAccountWriter.create(serviceAccount);
  }

  @Override
  public HasMetadata patch(StackGresDbOps context, HasMetadata newResource,
      HasMetadata oldResource) {
    oldResource.setMetadata(newResource.getMetadata());
    return serviceAccountWriter.update(safeCast(oldResource));
  }

  @Override
  public HasMetadata replace(StackGresDbOps context, HasMetadata resource) {
    return serviceAccountWriter.update(safeCast(resource));
  }

  @Override
  public void delete(StackGresDbOps context, HasMetadata resource) {
    serviceAccountWriter.delete(safeCast(resource));
  }

  private static ServiceAccount safeCast(HasMetadata resource) {
    if (!(resource instanceof ServiceAccount)) {
      throw new IllegalArgumentException("Resource must be a ServiceMonitor instance");
    }
    return (ServiceAccount) resource;
  }
}
