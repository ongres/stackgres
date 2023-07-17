/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import io.stackgres.operator.conciliation.AbstractRequiredResourceDecorator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterRequiredResourceDecorator
    extends AbstractRequiredResourceDecorator<StackGresClusterContext> {

  @Inject
  public ClusterRequiredResourceDecorator(
      DecoratorDiscoverer<StackGresClusterContext> decoratorDiscoverer,
      ResourceGenerationDiscoverer<StackGresClusterContext> generators) {
    super(decoratorDiscoverer, generators);
  }

}
