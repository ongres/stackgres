/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;

@ApplicationScoped
public class BackupConciliator extends AbstractConciliator<StackGresBackup> {

  @Inject
  public BackupConciliator(
      RequiredResourceGenerator<StackGresBackup> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresBackup> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache) {
    super(requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
  }

}
