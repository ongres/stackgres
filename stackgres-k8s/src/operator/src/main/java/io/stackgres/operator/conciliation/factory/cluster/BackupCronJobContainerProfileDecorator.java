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
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractContainerProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class BackupCronJobContainerProfileDecorator extends AbstractContainerProfileDecorator
    implements Decorator<StackGresClusterContext> {

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.BACKUP;
  }

  @Override
  public HasMetadata decorate(StackGresClusterContext context, HasMetadata resource) {
    if (Optional.of(context.getSource().getSpec())
        .map(StackGresClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getDisableClusterResourceRequirements)
        .orElse(false)) {
      return resource;
    }

    if (resource instanceof CronJob cronJob) {
      setProfileContainers(context.getProfile(),
          () -> Optional.of(cronJob)
          .map(CronJob::getSpec)
          .map(CronJobSpec::getJobTemplate)
          .map(JobTemplateSpec::getSpec)
          .map(JobSpec::getTemplate)
          .map(PodTemplateSpec::getSpec),
          Optional.ofNullable(context.getSource().getSpec().getPod().getResources())
          .map(StackGresClusterResources::getEnableClusterLimitsRequirements)
          .orElse(false),
          Optional.ofNullable(context.getSource().getSpec().getNonProductionOptions())
          .map(StackGresClusterNonProduction::getEnableSetClusterCpuRequests)
          .orElse(false),
          Optional.ofNullable(context.getSource().getSpec().getNonProductionOptions())
          .map(StackGresClusterNonProduction::getEnableSetClusterMemoryRequests)
          .orElse(false));
    }

    return resource;
  }

}
