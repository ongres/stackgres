/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder
public class DistributedLogsTemplatesConfigMap
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  static final String SUFFIX = "-logs-templates";

  public static String templatesName(StackGresDistributedLogs distributedLogs) {
    return ResourceUtil.resourceName(distributedLogs.getMetadata().getName() + SUFFIX);
  }

  private final LabelFactoryForDistributedLogs labelFactory;

  @Inject
  public DistributedLogsTemplatesConfigMap(
      LabelFactoryForDistributedLogs labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs distributedLogs = context.getSource();
    final ObjectMeta metadata = distributedLogs.getMetadata();
    final String namespace = metadata.getNamespace();

    return Stream.of(
        new ConfigMapBuilder()
        .withNewMetadata()
        .withLabels(labelFactory.genericLabels(distributedLogs))
        .withNamespace(namespace)
        .withName(templatesName(distributedLogs))
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(
            List.of(
                ClusterPath.LOCAL_BIN_START_FLUENTD_SH_PATH,
                ClusterPath.LOCAL_BIN_SHELL_UTILS_PATH)
            .stream()
            .collect(Collectors.toMap(
                c -> c.filename(),
                c -> Unchecked.supplier(() -> Resources
                    .asCharSource(DistributedLogsTemplatesConfigMap.class.getResource(
                        "/templates/" + c.filename()),
                        StandardCharsets.UTF_8)
                    .read()).get()))))
        .build());
  }

}
