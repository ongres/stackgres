/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;

@ApplicationScoped
public class ResourceGenerationDiscovererImpl
    extends ResourceDiscoverer<ResourceGenerator<DistributedLogsContext>>
    implements ResourceGenerationDiscoverer<DistributedLogsContext> {

  @Inject
  public ResourceGenerationDiscovererImpl(
      @Any
          Instance<ResourceGenerator<DistributedLogsContext>> instance) {
    init(instance);
  }

  @Override
  public List<ResourceGenerator<DistributedLogsContext>> getResourceGenerators(
      DistributedLogsContext context) {
    StackGresVersion version = StackGresVersion.getClusterStackGresVersion(context.getSource());
    return resourceHub.get(version);
  }
}
