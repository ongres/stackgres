/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.AbstractContainerProfileDecorator;
import io.stackgres.operator.conciliation.factory.Decorator;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class DbOpsJobContainerProfileDecorator extends AbstractContainerProfileDecorator
    implements Decorator<StackGresDbOpsContext> {

  @Override
  protected StackGresGroupKind getKind() {
    return StackGresGroupKind.DBOPS;
  }

  @Override
  public HasMetadata decorate(StackGresDbOpsContext context, HasMetadata resource) {
    if (DbOpsUtil.isAlreadyCompleted(context.getSource())
        || context.calculateDisableClusterResourceRequirements()) {
      return resource;
    }

    if (resource instanceof Job job) {
      setProfileContainers(
          context.getProfile(),
          Optional.ofNullable(context.getCluster().getSpec().getPods().getResources()),
          Optional.of(job)
          .map(Job::getSpec)
          .map(JobSpec::getTemplate)
          .map(PodTemplateSpec::getSpec));
    }

    return resource;
  }

}
