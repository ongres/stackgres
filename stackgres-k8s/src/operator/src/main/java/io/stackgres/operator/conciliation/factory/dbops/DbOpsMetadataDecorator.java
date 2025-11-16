/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.AbstractClusterMetadataDecorator;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class DbOpsMetadataDecorator
    extends AbstractClusterMetadataDecorator<StackGresDbOpsContext> {

  @Override
  protected Optional<StackGresClusterSpecMetadata> getSpecMetadata(StackGresDbOpsContext context) {
    return context.getFoundCluster().map(StackGresCluster::getSpec).map(StackGresClusterSpec::getMetadata);
  }

  @Override
  protected Optional<ObjectMeta> getMetadata(StackGresDbOpsContext context) {
    return Optional.of(context.getSource()).map(StackGresDbOps::getMetadata);
  }

}
