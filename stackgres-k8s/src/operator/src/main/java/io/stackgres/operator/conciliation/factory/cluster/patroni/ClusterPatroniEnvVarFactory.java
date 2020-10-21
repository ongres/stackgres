/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PatroniEnvironmentVariablesFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;

@Singleton
public class ClusterPatroniEnvVarFactory
    extends PatroniEnvironmentVariablesFactory<StackGresClusterContext>
    implements ResourceFactory<StackGresClusterContext, List<EnvVar>> {

  @Override
  public List<EnvVar> createResource(StackGresClusterContext context) {
    StackGresCluster cluster = context.getSource();
    return createPatroniEnvVars(cluster);
  }
}
