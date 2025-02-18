/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgstream.StackGresStream;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
public class Metrics {

  private static final String OPERATOR_METRIC_PREFIX = "operator_";

  private final MeterRegistry registry;

  private Map<Class<?>, Reconciliation> reconciliations;

  private Map<String, Number> gauges = new HashMap<>();

  static class Reconciliation {
    private long totalPerformed = 0;
    private long totalErrors = 0;
    private long lastDuration;
  }

  @Inject
  public Metrics(
      MeterRegistry registry) {
    this.registry = registry;
    reconciliations = Seq.<Class<?>>of(
        StackGresConfig.class,
        StackGresCluster.class,
        StackGresShardedCluster.class,
        StackGresDistributedLogs.class,
        StackGresBackup.class,
        StackGresDbOps.class,
        StackGresShardedBackup.class,
        StackGresShardedDbOps.class,
        StackGresScript.class,
        StackGresStream.class)
        .map(customResourceClass -> Tuple.tuple(customResourceClass, new Reconciliation()))
        .toMap(Tuple2::v1, Tuple2::v2);
  }

  public void incrementReconciliationTotalPerformed(
      Class<?> customResourceClass) {
    String singular = HasMetadata.getSingular(customResourceClass);
    reconciliations.get(customResourceClass).totalPerformed++;
    registry.gauge(
        OPERATOR_METRIC_PREFIX + "reconciliation_total_performed",
        List.of(new ImmutableTag("resource", singular)),
        this,
        metrics -> metrics.getReconciliationTotalPerformed(customResourceClass));
  }

  public long getReconciliationConfigTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresConfig.class);
  }

  public long getReconciliationClusterTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresCluster.class);
  }

  public long getReconciliationShardedClusterTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresShardedCluster.class);
  }

  public long getReconciliationDistributedLogsTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresDistributedLogs.class);
  }

  public long getReconciliationBackupTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresBackup.class);
  }

  public long getReconciliationDbOpsTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresDbOps.class);
  }

  public long getReconciliationShardedBackupTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresShardedBackup.class);
  }

  public long getReconciliationShardedDbOpsTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresShardedDbOps.class);
  }

  public long getReconciliationScriptTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresScript.class);
  }

  public long getReconciliationStreamTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresStream.class);
  }

  private long getReconciliationTotalPerformed(
      Class<?> customResourceClass) {
    return reconciliations.get(customResourceClass).totalPerformed;
  }

  public void incrementReconciliationTotalErrors(
      final Class<?> customResourceClass) {
    String singular = HasMetadata.getSingular(customResourceClass);
    reconciliations.get(customResourceClass).totalErrors++;
    registry.gauge(
        OPERATOR_METRIC_PREFIX + "reconciliation_total_errors",
        List.of(new ImmutableTag("resource", singular)),
        this,
        metrics -> metrics.getReconciliationTotalErrors(customResourceClass));
  }

  public long getReconciliationConfigTotalErrors() {
    return getReconciliationTotalErrors(StackGresConfig.class);
  }

  public long getReconciliationClusterTotalErrors() {
    return getReconciliationTotalErrors(StackGresCluster.class);
  }

  public long getReconciliationShardedClusterTotalErrors() {
    return getReconciliationTotalErrors(StackGresShardedCluster.class);
  }

  public long getReconciliationDistributedLogsTotalErrors() {
    return getReconciliationTotalErrors(StackGresDistributedLogs.class);
  }

  public long getReconciliationBackupTotalErrors() {
    return getReconciliationTotalErrors(StackGresBackup.class);
  }

  public long getReconciliationDbOpsTotalErrors() {
    return getReconciliationTotalErrors(StackGresDbOps.class);
  }

  public long getReconciliationShardedBackupTotalErrors() {
    return getReconciliationTotalErrors(StackGresShardedBackup.class);
  }

  public long getReconciliationShardedDbOpsTotalErrors() {
    return getReconciliationTotalErrors(StackGresShardedDbOps.class);
  }

  public long getReconciliationScriptTotalErrors() {
    return getReconciliationTotalErrors(StackGresScript.class);
  }

  public long getReconciliationStreamTotalErrors() {
    return getReconciliationTotalErrors(StackGresStream.class);
  }

  private long getReconciliationTotalErrors(
      Class<?> customResourceClass) {
    return reconciliations.get(customResourceClass).totalErrors;
  }

  public void setReconciliationLastDuration(
      final Class<?> customResourceClass,
      final long lastDuration) {
    String singular = HasMetadata.getSingular(customResourceClass);
    reconciliations.get(customResourceClass).lastDuration = lastDuration;
    registry.gauge(
        OPERATOR_METRIC_PREFIX + "reconciliation_last_duration",
        List.of(new ImmutableTag("resource", singular)),
        this,
        metrics -> metrics.getReconciliationLastDuration(customResourceClass));
  }

  public long getReconciliationConfigLastDuration() {
    return getReconciliationLastDuration(StackGresConfig.class);
  }

  public long getReconciliationClusterLastDuration() {
    return getReconciliationLastDuration(StackGresCluster.class);
  }

  public long getReconciliationShardedClusterLastDuration() {
    return getReconciliationLastDuration(StackGresShardedCluster.class);
  }

  public long getReconciliationDistributedLogsLastDuration() {
    return getReconciliationLastDuration(StackGresDistributedLogs.class);
  }

  public long getReconciliationBackupLastDuration() {
    return getReconciliationLastDuration(StackGresBackup.class);
  }

  public long getReconciliationDbOpsLastDuration() {
    return getReconciliationLastDuration(StackGresDbOps.class);
  }

  public long getReconciliationShardedBackupLastDuration() {
    return getReconciliationLastDuration(StackGresShardedBackup.class);
  }

  public long getReconciliationShardedDbOpsLastDuration() {
    return getReconciliationLastDuration(StackGresShardedDbOps.class);
  }

  public long getReconciliationScriptLastDuration() {
    return getReconciliationLastDuration(StackGresScript.class);
  }

  public long getReconciliationStreamLastDuration() {
    return getReconciliationLastDuration(StackGresStream.class);
  }

  private long getReconciliationLastDuration(
      Class<?> customResourceClass) {
    return reconciliations.get(customResourceClass).lastDuration;
  }

  public void gauge(String attributeName, Number attributeValueNumber) {
    String attributeNameNormalized = Pattern.compile(".")
        .matcher(attributeName)
        .results()
        .map(result -> Tuple.tuple(result.group(), result.group().toLowerCase(Locale.US)))
        .map(t -> t.v1.equals(t.v2) ? t.v1 : "_" + t.v2)
        .collect(Collectors.joining())
        .replaceAll("^_", "");
    final String name = OPERATOR_METRIC_PREFIX + attributeNameNormalized;
    gauges.put(name, attributeValueNumber);
    registry.gauge(name, this, metrics -> metrics.getGauge(name));
  }

  public double getGauge(String key) {
    Number gauge = this.gauges.get(key);
    if (gauge == null) {
      return Double.NaN;
    }
    return gauge.doubleValue();
  }

}
