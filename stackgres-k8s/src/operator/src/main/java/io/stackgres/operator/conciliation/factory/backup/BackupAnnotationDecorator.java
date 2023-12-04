/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import javax.inject.Singleton;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.AbstractClusterAnnotationDecorator;

@Singleton
@OperatorVersionBinder
public class BackupAnnotationDecorator
    extends AbstractClusterAnnotationDecorator<StackGresBackupContext> {

  @Override
  protected StackGresCluster getCluster(StackGresBackupContext context) {
    return context.getCluster();
  }

}
