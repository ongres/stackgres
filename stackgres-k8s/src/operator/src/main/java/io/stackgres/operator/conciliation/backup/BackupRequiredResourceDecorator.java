/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import io.stackgres.operator.conciliation.AbstractRequiredResourceDecorator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
