/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.AbstractClusterAnnotationDecorator;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class BackupAnnotationDecorator
    extends AbstractClusterAnnotationDecorator<StackGresBackupContext> {

  @Override
  protected Optional<StackGresClusterSpecMetadata> getSpecMetadata(StackGresBackupContext context) {
    return context.getFoundCluster().map(StackGresCluster::getSpec).map(StackGresClusterSpec::getMetadata);
  }

  @Override
  protected Optional<ObjectMeta> getMetadata(StackGresBackupContext context) {
    return Optional.of(context.getSource()).map(StackGresBackup::getMetadata);
  }

}
