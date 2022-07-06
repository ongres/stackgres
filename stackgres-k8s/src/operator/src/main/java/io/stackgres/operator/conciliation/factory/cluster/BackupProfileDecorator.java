/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.Optional;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1beta1.JobTemplateSpec;
import io.stackgres.common.StackGresKind;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.AbstractProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class BackupProfileDecorator extends AbstractProfileDecorator
    implements Decorator<StackGresClusterContext> {

  @Override
  protected StackGresKind getKind() {
    return StackGresKind.BACKUP;
  }

  @Override
  public void decorate(StackGresClusterContext context, Iterable<? extends HasMetadata> resources) {
    if (Optional.of(context.getSource().getSpec())
        .map(StackGresClusterSpec::getNonProductionOptions)
        .map(StackGresClusterNonProduction::getDisablePatroniResourceRequirements)
        .orElse(false)) {
      return;
    }

    Seq.seq(resources)
        .filter(CronJob.class::isInstance)
        .map(CronJob.class::cast)
        .findFirst()
        .ifPresent(cronJob -> setProfileContainers(context.getProfile(),
            () -> Optional.of(cronJob)
            .map(CronJob::getSpec)
            .map(CronJobSpec::getJobTemplate)
            .map(JobTemplateSpec::getSpec)
            .map(JobSpec::getTemplate)
            .map(PodTemplateSpec::getSpec)));
  }

}
