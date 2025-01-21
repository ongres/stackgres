/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.ClusterDefaultScripts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class PatroniDefaultScript implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster labelFactory;
  private final ClusterDefaultScripts patroniDefaultScripts;

  @Inject
  public PatroniDefaultScript(LabelFactoryForCluster labelFactory,
      ClusterDefaultScripts patroniDefaultScripts) {
    this.labelFactory = labelFactory;
    this.patroniDefaultScripts = patroniDefaultScripts;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream.of(getDefaultScript(context.getSource()));
  }

  private StackGresScript getDefaultScript(StackGresCluster cluster) {
    return new StackGresScriptBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(ManagedSqlUtil.defaultName(cluster))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withNewSpec()
        .withScripts(patroniDefaultScripts.getDefaultScripts(cluster))
        .endSpec()
        .build();
  }

}
