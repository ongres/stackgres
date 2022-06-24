/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.ArrayList;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.ClusterDefaultScripts;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V_1_3)
public class PatroniDefaultScript implements ResourceGenerator<StackGresClusterContext> {

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;
  private final ClusterDefaultScripts patroniDefaultScripts;

  @Inject
  public PatroniDefaultScript(LabelFactoryForCluster<StackGresCluster> labelFactory,
      ClusterDefaultScripts patroniDefaultScripts) {
    this.labelFactory = labelFactory;
    this.patroniDefaultScripts = patroniDefaultScripts;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream.of(getDefaultScript(context.getSource()));
  }

  private StackGresScript getDefaultScript(StackGresCluster cluster) {
    StackGresScript defaultScript = new StackGresScript();
    defaultScript.setMetadata(new ObjectMeta());
    defaultScript.getMetadata().setNamespace(cluster.getMetadata().getNamespace());
    defaultScript.getMetadata().setName(ManagedSqlUtil.defaultName(cluster));
    defaultScript.getMetadata().setLabels(labelFactory.genericLabels(cluster));
    defaultScript.setSpec(new StackGresScriptSpec());
    defaultScript.getSpec().setScripts(new ArrayList<>());
    defaultScript.getSpec().getScripts().addAll(patroniDefaultScripts.getDefaultScripts(cluster));
    return defaultScript;
  }

}
