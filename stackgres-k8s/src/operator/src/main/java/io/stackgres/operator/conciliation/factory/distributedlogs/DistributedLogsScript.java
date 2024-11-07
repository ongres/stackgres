/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.cluster.ClusterDefaultScripts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder
public class DistributedLogsScript
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  private final LabelFactoryForDistributedLogs labelFactory;

  public static String scriptName(StackGresDistributedLogs distributedLogs) {
    return distributedLogs.getMetadata().getName() + "-init";
  }

  @Inject
  public DistributedLogsScript(LabelFactoryForDistributedLogs labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    return Seq.of(getScript(context));
  }
  
  private StackGresScript getScript(StackGresDistributedLogsContext context) {
    StackGresDistributedLogs distributedLogs = context.getSource();
    return new StackGresScriptBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(distributedLogs.getMetadata().getNamespace())
            .withName(scriptName(distributedLogs))
            .withLabels(labelFactory.genericLabels(distributedLogs))
            .build())
        .editSpec()
        .addNewScript()
        .withName("distributed-logs-template")
        .withRetryOnError(true)
        .withDatabase("template1")
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(ClusterDefaultScripts.class.getResource(
                "/distributed-logs-template.sql"),
                StandardCharsets.UTF_8)
            .read()).get())
        .endScript()
        .endSpec()
        .build();
  }

}
