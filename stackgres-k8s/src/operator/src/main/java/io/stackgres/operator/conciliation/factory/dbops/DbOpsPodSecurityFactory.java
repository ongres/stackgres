/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbOpsPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresDbOpsContext, PodSecurityContext> {

  @Inject
  public DbOpsPodSecurityFactory(OperatorPropertyContext operatorContext) {
    super(operatorContext);
  }

  @Override
  public PodSecurityContext createResource(StackGresDbOpsContext source) {
    return createPodSecurityContext();
  }

}
