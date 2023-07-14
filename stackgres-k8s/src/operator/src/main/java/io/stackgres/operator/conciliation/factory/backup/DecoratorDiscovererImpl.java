/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import java.util.List;

import io.stackgres.operator.conciliation.ResourceDiscoverer;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class DecoratorDiscovererImpl
    extends ResourceDiscoverer<Decorator<StackGresBackupContext>>
    implements DecoratorDiscoverer<StackGresBackupContext> {

  @Inject
  public DecoratorDiscovererImpl(
      @Any Instance<Decorator<StackGresBackupContext>> instance) {
    init(instance);
  }

  @Override
  public List<Decorator<StackGresBackupContext>> discoverDecorator(StackGresBackupContext context) {
    return resourceHub.get(context.getVersion()).stream().toList();

  }
}
