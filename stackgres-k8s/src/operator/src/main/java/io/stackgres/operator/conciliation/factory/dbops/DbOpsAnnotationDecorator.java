/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import javax.inject.Singleton;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.AbstractClusterAnnotationDecorator;

@Singleton
@OperatorVersionBinder
public class DbOpsAnnotationDecorator
    extends AbstractClusterAnnotationDecorator<StackGresDbOpsContext> {

  @Override
  protected StackGresCluster getCluster(StackGresDbOpsContext context) {
    return context.getCluster();
  }

}
