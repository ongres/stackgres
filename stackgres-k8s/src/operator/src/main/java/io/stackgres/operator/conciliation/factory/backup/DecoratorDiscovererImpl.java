/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.AbstractResourceDiscoverer;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

@ApplicationScoped
public class DecoratorDiscovererImpl
    extends AbstractResourceDiscoverer<Decorator<StackGresBackupContext>>
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
