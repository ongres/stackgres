/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;

@ApplicationScoped
public class DbOpsPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresDbOpsContext, PodSecurityContext> {

  @Override
  public PodSecurityContext createResource(StackGresDbOpsContext source) {
    return createPodSecurityContext();
  }

}
