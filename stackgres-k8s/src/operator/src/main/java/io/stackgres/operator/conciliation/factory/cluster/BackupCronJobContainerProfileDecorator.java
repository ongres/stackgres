/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractContainerProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class BackupCronJobContainerProfileDecorator extends AbstractContainerProfileDecorator
    implements Decorator<StackGresClusterContext> {

  private final DefaultProfileFactory defaultProfileFactory;

  public BackupCronJobContainerProfileDecorator(DefaultProfileFactory defaultProfileFactory) {
    this.defaultProfileFactory = defaultProfileFactory;
  }

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.BACKUP;
  }

  @Override
  public HasMetadata decorate(StackGresClusterContext context, HasMetadata resource) {
    if (context.calculateDisableClusterResourceRequirements()) {
      return resource;
    }

    if (resource instanceof CronJob cronJob) {
      setProfileContainers(
          context.getProfile()
          .orElseGet(() -> defaultProfileFactory.buildResource(context.getSource())),
          Optional.ofNullable(context.getCluster().getSpec().getPods().getResources()),
          Optional.of(cronJob)
          .map(CronJob::getSpec)
          .map(CronJobSpec::getJobTemplate)
          .map(JobTemplateSpec::getSpec)
          .map(JobSpec::getTemplate)
          .map(PodTemplateSpec::getSpec));
    }

    return resource;
  }

}
