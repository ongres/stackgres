/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
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
@OperatorVersionBinder
public class DistributedLogsService
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  private static final String SUFFIX = "-fluentd";

  private final LabelFactoryForDistributedLogs labelFactory;

  public static String serviceName(StackGresDistributedLogs distributedLogs) {
    return serviceName(distributedLogs.getMetadata().getName());
  }

  public static String serviceName(String distributedLogsName) {
    return ResourceUtil.resourceName(distributedLogsName + SUFFIX);
  }

  @Inject
  public DistributedLogsService(LabelFactoryForDistributedLogs labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    return Seq.of(getService(context));
  }
  
  private Service getService(StackGresDistributedLogsContext context) {
    StackGresDistributedLogs distributedLogs = context.getSource();
    return new ServiceBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(distributedLogs.getMetadata().getNamespace())
            .withName(serviceName(distributedLogs))
            .withLabels(labelFactory.genericLabels(distributedLogs))
            .build())
        .editSpec()
        .withType("ExternalName")
        .withExternalName(
            PatroniUtil.readWriteName(DistributedLogsCluster.getCluster(
                labelFactory, distributedLogs, Optional.empty()))
            + "." + distributedLogs.getMetadata().getNamespace()
            + StackGresUtil.domainSearchPath())
        .endSpec()
        .build();
  }

}
