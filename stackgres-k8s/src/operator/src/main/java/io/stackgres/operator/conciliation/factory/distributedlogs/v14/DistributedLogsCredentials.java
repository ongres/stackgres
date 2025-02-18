/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.v14;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(stopAt = StackGresVersion.V_1_14)
public class DistributedLogsCredentials
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  private static final String SUFFIX = "-credentials-v-1-14";

  private final LabelFactoryForDistributedLogs labelFactory;

  public static String secretName(StackGresDistributedLogs distributedLogs) {
    return ResourceUtil.resourceName(distributedLogs.getMetadata().getName() + SUFFIX);
  }

  @Inject
  public DistributedLogsCredentials(LabelFactoryForDistributedLogs labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    return Seq.of(getSecret(context));
  }
  
  private Secret getSecret(StackGresDistributedLogsContext context) {
    StackGresDistributedLogs distributedLogs = context.getSource();
    return new SecretBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(distributedLogs.getMetadata().getNamespace())
            .withName(secretName(distributedLogs))
            .withLabels(labelFactory.genericLabels(distributedLogs))
            .build())
        .withData(context.getDatabaseSecret()
            .map(Secret::getData)
            .orElseThrow())
        .build();
  }

}
