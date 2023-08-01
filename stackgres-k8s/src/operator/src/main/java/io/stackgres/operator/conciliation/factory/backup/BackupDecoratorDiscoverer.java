/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.AbstractDecoratorDiscoverer;
import io.stackgres.operator.conciliation.factory.Decorator;

@ApplicationScoped
public class BackupDecoratorDiscoverer
    extends AbstractDecoratorDiscoverer<StackGresBackupContext> {

  @Inject
  public BackupDecoratorDiscoverer(
      @Any Instance<Decorator<StackGresBackupContext>> instance) {
    super(instance);
  }

}
