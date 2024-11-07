/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static io.stackgres.common.FluentdUtil.PATRONI_LOG_TYPE;
import static io.stackgres.common.FluentdUtil.POSTGRES_LOG_TYPE;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.FluentdUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.distributedlogs.PatroniTableFields;
import io.stackgres.common.distributedlogs.PostgresTableFields;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.fluentbit.FluentBit;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@OperatorVersionBinder
public class DistributedLogsFlunetdConfigMap
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  static final String PATRONI_TABLE_FIELDS = Stream.of(PatroniTableFields.values())
      .map(PatroniTableFields::getFieldName)
      .collect(Collectors.joining(","));
  static final String POSTGRES_TABLE_FIELDS = Stream.of(PostgresTableFields.values())
      .map(PostgresTableFields::getFieldName)
      .collect(Collectors.joining(","));
  private static final Logger FLEUNTD_LOGGER = LoggerFactory.getLogger("io.stackgres.fleuntd");

  private final LabelFactoryForDistributedLogs labelFactory;

  @Inject
  public DistributedLogsFlunetdConfigMap(
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
        .withName(FluentdUtil.configName(distributedLogs))
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(Map.of(
            "fluentd.conf", getFluentdConfig(context))))
        .build());
  }

  private String getFluentdConfig(final StackGresDistributedLogsContext distributedLogsContext) {
    return ""
        + "<system>\n"
        + "  workers " + getMaxWorkersBeforeRestart(distributedLogsContext) + "\n"
        + "</system>\n"
        + "\n"
        + "<worker 0>\n"
        + "  <source>\n"
        + "    @type forward\n"
        + "    bind 0.0.0.0\n"
        + "    port " + FluentdUtil.FORWARD_PORT + "\n"
        + "  </source>\n"
        + "\n"
        + "  <filter *.*.*.*.*>\n"
        + "    @type record_transformer\n"
        + "    enable_ruby\n"
        + "    <record>\n"
        + "      pod_name ${record[\"kubernetes\"][\"pod_name\"]}\n"
        + "    </record>\n"
        + "    <record>\n"
        + "      role ${record[\"kubernetes\"][\"labels\"][\"role\"]}\n"
        + "    </record>\n"
        + "  </filter>"
        + "\n"
        + Seq.seq(distributedLogsContext.getConnectedClusters())
        .zipWithIndex()
        .map(t -> t.map2(index -> index + getCoreWorkers()))
        .map(t -> ""
            + "  <match " + FluentBit.tagName(t.v1, "*") + ".*.*>\n"
            + "    @type forward\n"
            + "    <buffer>\n"
            + "      @type file\n"
            + "      path /var/log/fluentd/" + FluentBit.tagName(t.v1, "buffer") + "\n"
            + "    </buffer>\n"
            + "    <server>\n"
            + "      name localhost\n"
            + "      host 127.0.0.1\n"
            + "      port " + (FluentdUtil.FORWARD_PORT + t.v2) + "\n"
            + "    </server>\n"
            + "  </match>\n"
            + "\n")
        .collect(Collectors.joining("\n"))
        + "  <match *.*.*.*.*>\n"
        + "    @type forward\n"
        + "    <buffer>\n"
        + "      @type file\n"
        + "      path /var/log/fluentd/loop.buffer\n"
        + "    </buffer>\n"
        + "    <server>\n"
        + "      name localhost\n"
        + "      host 127.0.0.1\n"
        + "      port " + FluentdUtil.FORWARD_PORT + "\n"
        + "    </server>\n"
        + "  </match>\n"
        + "</worker>\n"
        + "\n"
        + Seq.seq(distributedLogsContext.getConnectedClusters())
        .zipWithIndex()
        .map(t -> t.map2(index -> index + getCoreWorkers()))
        .map(t -> ""
            + "<worker " + t.v2 + ">\n"
            + "  <source>\n"
            + "    @type forward\n"
            + "    bind 127.0.0.1\n"
            + "    port " + (FluentdUtil.FORWARD_PORT + t.v2) + "\n"
            + "  </source>\n"
            + "\n"
            + "  <match " + FluentBit.tagName(t.v1, POSTGRES_LOG_TYPE) + ".*.*>\n"
            + "    @type copy\n"
            + "    <store>\n"
            + "      @type sql\n"
            + "      host /var/run/postgresql\n"
            + "      port " + EnvoyUtil.PG_PORT + "\n"
            + "      database " + FluentdUtil.databaseName(t.v1) + "\n"
            + "      adapter postgresql\n"
            + "      username postgres\n"
            + "      <table>\n"
            + "        table log_postgres\n"
            + "        column_mapping '" + POSTGRES_TABLE_FIELDS + "'\n"
            + "      </table>\n"
            + "    </store>\n"
            + (FLEUNTD_LOGGER.isDebugEnabled()
            ? "    <store>\n"
            + "      @type stdout\n"
            + "      @log_level " + (FLEUNTD_LOGGER.isTraceEnabled() ? "info" : "debug") + "\n"
            + "    </store>\n" : "")
            + "  </match>\n"
            + "\n"
            + "  <match " + FluentBit.tagName(t.v1, PATRONI_LOG_TYPE) + ".*.*>\n"
            + "    @type copy\n"
            + "    <store>\n"
            + "      @type sql\n"
            + "      host /var/run/postgresql\n"
            + "      port " + EnvoyUtil.PG_PORT + "\n"
            + "      database " + FluentdUtil.databaseName(t.v1) + "\n"
            + "      adapter postgresql\n"
            + "      username postgres\n"
            + "      <table>\n"
            + "        table log_patroni\n"
            + "        column_mapping '" + PATRONI_TABLE_FIELDS + "'\n"
            + "      </table>\n"
            + "    </store>\n"
            + (FLEUNTD_LOGGER.isDebugEnabled()
            ? "    <store>\n"
            + "      @type stdout\n"
            + "      @log_level " + (FLEUNTD_LOGGER.isTraceEnabled() ? "info" : "debug") + "\n"
            + "    </store>\n" : "")
            + "  </match>\n"
            + "</worker>\n"
            + "\n")
        .collect(Collectors.joining("\n"));
  }

  /**
   * When needed workers are more than 16 fluentd must be restarted.
   */
  private int getMaxWorkersBeforeRestart(
      final StackGresDistributedLogsContext distributedLogsContext) {
    return getWorkersBatchSize()
        * ((getCoreWorkers() + distributedLogsContext.getConnectedClusters().size()
        + getWorkersBatchSize() - 1) / getWorkersBatchSize());
  }

  private int getCoreWorkers() {
    return 1;
  }

  private int getWorkersBatchSize() {
    return 16;
  }

}
