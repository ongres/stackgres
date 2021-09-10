/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.operator.conciliation.AbstractDecoratorResource;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

@ApplicationScoped
public class BackupDecoratorResource
    extends AbstractDecoratorResource<StackGresBackupContext> {

  @Inject
  public BackupDecoratorResource(
      DecoratorDiscoverer<StackGresBackupContext> decoratorDiscoverer,
      ResourceGenerationDiscoverer<StackGresBackupContext> generators) {
    super(decoratorDiscoverer, generators);
  }

  public BackupDecoratorResource() {
    super(null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }
}
