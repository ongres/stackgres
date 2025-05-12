/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.AbstractContainerProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ShardedBackupJobContainerProfileDecorator extends AbstractContainerProfileDecorator
    implements Decorator<StackGresShardedBackupContext> {

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.BACKUP;
  }

  @Override
  public HasMetadata decorate(StackGresShardedBackupContext context, HasMetadata resource) {
    if (ShardedBackupJob.skipBackupJobCreation(context)
        || context.calculateDisableClusterResourceRequirements()) {
      return resource;
    }

    if (resource instanceof Job job) {
      setProfileContainers(
          context.getProfile(),
          Optional.ofNullable(context.getShardedCluster().getSpec().getCoordinator().getPods().getResources()),
          Optional.of(job)
          .map(Job::getSpec)
          .map(JobSpec::getTemplate)
          .map(PodTemplateSpec::getSpec));
    }

    return resource;
  }

}
