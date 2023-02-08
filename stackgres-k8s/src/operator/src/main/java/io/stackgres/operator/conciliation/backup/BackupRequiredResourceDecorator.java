/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.AbstractRequiredResourceDecorator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

@ApplicationScoped
public class BackupRequiredResourceDecorator
    extends AbstractRequiredResourceDecorator<StackGresBackupContext> {

  @Inject
  public BackupRequiredResourceDecorator(
      DecoratorDiscoverer<StackGresBackupContext> decoratorDiscoverer,
      ResourceGenerationDiscoverer<StackGresBackupContext> generators) {
    super(decoratorDiscoverer, generators);
  }

}
