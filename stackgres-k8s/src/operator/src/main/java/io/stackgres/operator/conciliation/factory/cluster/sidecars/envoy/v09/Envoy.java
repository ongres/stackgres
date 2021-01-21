/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy.v09;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.LabelFactory;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy.AbstractEnvoy;

@Singleton
@Sidecar(AbstractEnvoy.NAME)
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V093)
@RunningContainer(order = 4)
public class Envoy extends AbstractEnvoy {
  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider,
               LabelFactory<StackGresCluster> labelFactory) {
    super(yamlMapperProvider, labelFactory);
  }
}
