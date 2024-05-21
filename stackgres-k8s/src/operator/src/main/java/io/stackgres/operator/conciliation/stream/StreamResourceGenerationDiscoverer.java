/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import io.stackgres.operator.conciliation.AbstractResourceDiscoverer;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamResourceGenerationDiscoverer
    extends AbstractResourceDiscoverer<StackGresStreamContext> {

  @Inject
  public StreamResourceGenerationDiscoverer(
      @Any Instance<ResourceGenerator<StackGresStreamContext>> instance,
      DecoratorDiscoverer<StackGresStreamContext> decoratorDiscoverer) {
    super(instance, decoratorDiscoverer);
  }

}
