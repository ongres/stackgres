/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.metrics.AbstractMetrics;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
public class Metrics extends AbstractMetrics {

  static final String PATRONI_SUFFIX = "-patroni";

  private Map<String, Reconciliation> reconciliations;

  static class Reconciliation {
    private long totalPerformed = 0;
    private long totalErrors = 0;
    private long lastDuration;
  }

  @Inject
  public Metrics(
      MeterRegistry registry) {
    super(registry, "controller");
    reconciliations = Seq.<String>of(
        HasMetadata.getSingular(StackGresCluster.class),
        HasMetadata.getSingular(StackGresCluster.class) + PATRONI_SUFFIX)
        .map(customResourceClass -> Tuple.tuple(customResourceClass, new Reconciliation()))
        .toMap(Tuple2::v1, Tuple2::v2);
  }

  public void incrementPatroniReconciliationTotalPerformed(
      Class<?> customResourceClass) {
    incrementReconciliationTotalPerformed(customResourceClass, PATRONI_SUFFIX);
  }

  public void incrementReconciliationTotalPerformed(
      Class<?> customResourceClass) {
    incrementReconciliationTotalPerformed(customResourceClass, "");
  }

  private void incrementReconciliationTotalPerformed(
      Class<?> customResourceClass, String suffix) {
    String singular = HasMetadata.getSingular(customResourceClass);
    reconciliations.get(singular + suffix).totalPerformed++;
    registry.gauge(
        prefix + "reconciliation_total_performed",
        List.of(new ImmutableTag("resource", singular + suffix)),
        this,
        metrics -> metrics.getReconciliationTotalPerformed(customResourceClass, suffix));
  }

  public long getReconciliationClusterTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresCluster.class, "");
  }

  public long getReconciliationPatroniClusterTotalPerformed() {
    return getReconciliationTotalPerformed(StackGresCluster.class, PATRONI_SUFFIX);
  }

  private long getReconciliationTotalPerformed(
      Class<?> customResourceClass, String suffix) {
    String singular = HasMetadata.getSingular(customResourceClass);
    return reconciliations.get(singular + suffix).totalPerformed;
  }

  public void incrementPatroniReconciliationTotalErrors(
      final Class<?> customResourceClass) {
    incrementReconciliationTotalErrors(customResourceClass, PATRONI_SUFFIX);
  }

  public void incrementReconciliationTotalErrors(
      final Class<?> customResourceClass) {
    incrementReconciliationTotalErrors(customResourceClass, "");
  }

  private void incrementReconciliationTotalErrors(
      final Class<?> customResourceClass,
      String suffix) {
    String singular = HasMetadata.getSingular(customResourceClass);
    reconciliations.get(singular + suffix).totalErrors++;
    registry.gauge(
        prefix + "reconciliation_total_errors",
        List.of(new ImmutableTag("resource", singular + suffix)),
        this,
        metrics -> metrics.getReconciliationTotalErrors(customResourceClass, suffix));
  }

  public long getReconciliationClusterTotalErrors() {
    return getReconciliationTotalErrors(StackGresCluster.class, "");
  }

  public long getReconciliationPatroniClusterTotalErrors() {
    return getReconciliationTotalErrors(StackGresCluster.class, PATRONI_SUFFIX);
  }

  private long getReconciliationTotalErrors(
      Class<?> customResourceClass,
      String suffix) {
    String singular = HasMetadata.getSingular(customResourceClass);
    return reconciliations.get(singular + suffix).totalErrors;
  }

  public void setPatroniReconciliationLastDuration(
      final Class<?> customResourceClass,
      final long lastDuration) {
    setReconciliationLastDuration(customResourceClass, PATRONI_SUFFIX, lastDuration);
  }

  public void setReconciliationLastDuration(
      final Class<?> customResourceClass,
      final long lastDuration) {
    setReconciliationLastDuration(customResourceClass, "", lastDuration);
  }

  private void setReconciliationLastDuration(
      final Class<?> customResourceClass,
      final String suffix,
      final long lastDuration) {
    String singular = HasMetadata.getSingular(customResourceClass);
    reconciliations.get(singular + suffix).lastDuration = lastDuration;
    registry.gauge(
        prefix + "reconciliation_last_duration",
        List.of(new ImmutableTag("resource", singular + suffix)),
        this,
        metrics -> metrics.getReconciliationLastDuration(customResourceClass, suffix));
  }

  public long getReconciliationClusterLastDuration() {
    return getReconciliationLastDuration(StackGresCluster.class, "");
  }

  public long getReconciliationPatroniClusterLastDuration() {
    return getReconciliationLastDuration(StackGresCluster.class, PATRONI_SUFFIX);
  }

  private long getReconciliationLastDuration(
      Class<?> customResourceClass,
      String suffix) {
    String singular = HasMetadata.getSingular(customResourceClass);
    return reconciliations.get(suffix + singular).lastDuration;
  }

}
