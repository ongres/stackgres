/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import java.util.Optional;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.AbstractContainerProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class BackupJobContainerProfileDecorator extends AbstractContainerProfileDecorator
    implements Decorator<StackGresBackupContext> {

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.BACKUP;
  }

  @Override
  public void decorate(StackGresBackupContext context, Iterable<? extends HasMetadata> resources) {
    if (BackupJob.skipBackupJobCreation(context)
        || Optional.of(context.getCluster().getSpec())
        .map(StackGresClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getDisableClusterResourceRequirements)
        .orElse(false)) {
      return;
    }

    Seq.seq(resources)
        .filter(Job.class::isInstance)
        .map(Job.class::cast)
        .forEach(job -> setProfileContainers(context.getProfile(),
            () -> Optional.of(job)
            .map(Job::getSpec)
            .map(JobSpec::getTemplate)
            .map(PodTemplateSpec::getSpec),
            Optional.ofNullable(context.getCluster().getSpec().getPod().getResources())
            .map(StackGresClusterResources::getEnableClusterLimitsRequirements)
            .orElse(false),
            Optional.ofNullable(context.getCluster().getSpec().getNonProductionOptions())
            .map(StackGresClusterNonProduction::getEnableSetClusterCpuRequests)
            .orElse(false),
            Optional.ofNullable(context.getCluster().getSpec().getNonProductionOptions())
            .map(StackGresClusterNonProduction::getEnableSetClusterMemoryRequests)
            .orElse(false)));
  }

}
