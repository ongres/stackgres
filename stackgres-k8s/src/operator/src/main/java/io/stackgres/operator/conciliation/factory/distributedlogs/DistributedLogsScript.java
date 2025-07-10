/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static io.stackgres.operator.common.StackGresDistributedLogsUtil.TIMESCALEDB_EXTENSION_NAME;
import static io.stackgres.operator.common.StackGresDistributedLogsUtil.getDefaultDistributedLogsExtensions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptBuilder;
import io.stackgres.common.distributedlogs.Tables;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder
public class DistributedLogsScript
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  private static final String TOMORROW_PATTERN_FORMAT = "yyyy-MM-dd";

  private final LabelFactoryForDistributedLogs labelFactory;

  public static String scriptName(StackGresDistributedLogs distributedLogs) {
    return distributedLogs.getMetadata().getName() + "-scripts";
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
    final String timescaledbVersion = Optional.ofNullable(distributedLogs.getStatus())
        .map(StackGresDistributedLogsStatus::getTimescaledbVersion)
        .or(() -> getDefaultDistributedLogsExtensions(distributedLogs)
            .stream()
            .filter(extension -> extension.extensionName().equals(TIMESCALEDB_EXTENSION_NAME))
            .map(ExtensionTuple::extensionVersion)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst())
        .orElse(null);
    final String isPendingRestart = String.valueOf(context.getCluster()
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .anyMatch(ClusterStatusCondition.POD_REQUIRES_RESTART::isCondition));
    final String databaseList = context.getConnectedClusters().stream()
        .map(cluster -> "'" + FluentdUtil.databaseName(
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName()) + "'")
        .collect(Collectors.joining(","));
    final String databaseAndRetenentionList = context.getConnectedClusters().stream()
        .map(cluster -> "['" + FluentdUtil.databaseName(
            cluster.getMetadata().getNamespace(),
            cluster.getMetadata().getName()) + "','"
            + Optional
            .ofNullable(cluster.getSpec().getDistributedLogs().getRetention())
            .orElse("7 days") + "']")
        .collect(Collectors.joining(","));
    final String tableList = Arrays.asList(Tables.values())
        .stream()
        .map(Tables::getTableName)
        .map(tableName -> "'" + tableName + "'")
        .collect(Collectors.joining(","));
    final String tomorrow = DateTimeFormatter.ofPattern(TOMORROW_PATTERN_FORMAT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.now().plus(Duration.ofDays(1)));

    return new StackGresScriptBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(distributedLogs.getMetadata().getNamespace())
            .withName(scriptName(distributedLogs))
            .withLabels(labelFactory.genericLabels(distributedLogs))
            .build())
        .editSpec()
        .addNewScript()
        .withId(0)
        .withName("install-extensions")
        .withRetryOnError(true)
        .withDatabase("postgres")
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(DistributedLogsScript.class.getResource(
                "/distributed-logs/install-extensions.sql"),
                StandardCharsets.UTF_8)
            .read()).get().formatted(
                Optional.of(databaseList).filter(Predicate.not(String::isEmpty)).orElse("null"),
                timescaledbVersion,
                isPendingRestart))
        .endScript()
        .addNewScript()
        .withId(1)
        .withName("init")
        .withRetryOnError(true)
        .withDatabase("postgres")
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(DistributedLogsScript.class.getResource(
                "/distributed-logs/init.sql"),
                StandardCharsets.UTF_8)
            .read()).get().formatted(
                Optional.of(databaseList).filter(Predicate.not(String::isEmpty)).orElse("null")))
        .endScript()
        .addNewScript()
        .withId(2)
        .withName("update-databases")
        .withRetryOnError(true)
        .withDatabase("postgres")
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(DistributedLogsScript.class.getResource(
                "/distributed-logs/update-databases.sql"),
                StandardCharsets.UTF_8)
            .read()).get().formatted(
                databaseList))
        .endScript()
        .addNewScript()
        .withId(3)
        .withName("reconcile-retention")
        .withRetryOnError(true)
        .withDatabase("postgres")
        .withScript(Unchecked.supplier(() -> Resources
            .asCharSource(DistributedLogsScript.class.getResource(
                "/distributed-logs/reconcile-retention.sql"),
                StandardCharsets.UTF_8)
            .read()).get().formatted(
                databaseAndRetenentionList,
                tableList,
                tomorrow))
        .endScript()
        .endSpec()
        .build();
  }

}
