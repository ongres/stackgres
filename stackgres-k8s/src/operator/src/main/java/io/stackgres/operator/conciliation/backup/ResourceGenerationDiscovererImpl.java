/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerator;

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
