/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.List;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class ResourceGenerationDiscovererImpl
    extends ResourceDiscoverer<ResourceGenerator<StackGresBackupContext>>
    implements ResourceGenerationDiscoverer<StackGresBackupContext> {

  @Inject
  public ResourceGenerationDiscovererImpl(
      @Any
          Instance<ResourceGenerator<StackGresBackupContext>> instance) {
    init(instance);
  }

  @Override
  public List<ResourceGenerator<StackGresBackupContext>> getResourceGenerators(
      StackGresBackupContext context) {
    return resourceHub.get(context.getVersion());
  }
}
