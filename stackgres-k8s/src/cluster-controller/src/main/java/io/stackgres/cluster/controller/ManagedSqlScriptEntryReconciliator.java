/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static io.stackgres.common.RetryUtil.calculateExponentialBackoffDelay;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import org.jooq.lambda.Seq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedSqlScriptEntryReconciliator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ManagedSqlScriptEntryReconciliator.class);

  private final ManagedSqlReconciliator managedSqlReconciliator;
  private final KubernetesClient client;
  private final StackGresClusterContext context;
  private final ManagedSqlScriptEntry managedSqlScriptEntry;

  protected ManagedSqlScriptEntryReconciliator(ManagedSqlReconciliator managedSqlReconciliator,
      KubernetesClient client, StackGresClusterContext context,
      ManagedSqlScriptEntry managedSqlScriptEntry) {
    super();
    this.managedSqlReconciliator = managedSqlReconciliator;
    this.client = client;
    this.context = context;
    this.managedSqlScriptEntry = managedSqlScriptEntry;
  }

  @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
      justification = "This is the feature not a bug")
  protected boolean reconcile() {
    final var managedScriptEntryStatus = getOrCreateScriptEntryStatus();
    if ((managedSqlReconciliator.isScriptEntryExecutionHang(
        managedSqlScriptEntry.getScriptEntry(), managedScriptEntryStatus)
        || managedSqlReconciliator.isScriptEntryFailed(
            managedSqlScriptEntry.getScriptEntry(), managedScriptEntryStatus))
        && !managedSqlScriptEntry.getScriptEntry().getRetryOnErrorOrDefault()) {
      return false;
    }
    managedScriptEntryStatus.setVersion(managedSqlScriptEntry.getScriptEntry().getVersion());
    if (isExecutionBackOff(managedScriptEntryStatus)) {
      LOGGER.warn("Back-off execution for managed script {}",
          managedSqlScriptEntry.getManagedScriptEntryDescription());
      return false;
    }
    final String sql = managedSqlReconciliator.getSql(
        context, managedSqlScriptEntry.getScriptEntry());
    if (!isSqlSameStatusHash(sql)) {
      LOGGER.warn("Skipping execution due to hash mismatch for managed script {}",
          managedSqlScriptEntry.getManagedScriptEntryDescription());
      return false;
    }
    executeScriptEntry(managedScriptEntryStatus, sql);
    managedSqlReconciliator.updateManagedSqlStatus(context,
        managedSqlScriptEntry.getManagedSqlStatus());
    boolean isScriptEntryUpToDate = managedSqlReconciliator.isScriptEntryUpToDate(
        managedSqlScriptEntry.getScriptEntry(), managedSqlScriptEntry.getManagedScriptStatus());
    managedSqlReconciliator.sendEvent(client, context,
        managedSqlScriptEntry.getManagedScript(), managedSqlScriptEntry.getScriptEntry(),
        managedScriptEntryStatus, isScriptEntryUpToDate);
    return isScriptEntryUpToDate;
  }

  private StackGresClusterManagedScriptEntryScriptStatus getOrCreateScriptEntryStatus() {
    var foundScriptEntryStatus = Optional.of(managedSqlScriptEntry.getManagedScriptStatus())
        .map(StackGresClusterManagedScriptEntryStatus::getScripts)
        .stream().flatMap(List::stream)
        .filter(anScriptEntryStatus -> Objects.equals(
            managedSqlScriptEntry.getScriptEntry().getId(), anScriptEntryStatus.getId()))
        .findFirst();
    final StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus;
    if (foundScriptEntryStatus.isPresent()) {
      managedScriptEntryStatus = foundScriptEntryStatus.get();
    } else {
      managedScriptEntryStatus = new StackGresClusterManagedScriptEntryScriptStatus();
      managedScriptEntryStatus.setId(managedSqlScriptEntry.getScriptEntry().getId());
      if (managedSqlScriptEntry.getManagedScriptStatus().getScripts() == null) {
        managedSqlScriptEntry.getManagedScriptStatus().setScripts(new ArrayList<>());
      }
      managedSqlScriptEntry.getManagedScriptStatus().getScripts().add(managedScriptEntryStatus);
    }
    return managedScriptEntryStatus;
  }

  private boolean isExecutionBackOff(
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus) {
    if (managedSqlReconciliator.isScriptEntryFailed(
            managedSqlScriptEntry.getScriptEntry(), managedScriptEntryStatus)
        && managedSqlScriptEntry.getManagedScriptStatus().getFailedAt() != null) {
      return Duration.between(Instant.parse(
              managedSqlScriptEntry.getManagedScriptStatus().getFailedAt()),
              Instant.now()).getSeconds() >= calculateExponentialBackoffDelay(
                  0, 600, 60, managedScriptEntryStatus.getIntents());
    }
    if (managedSqlReconciliator.isScriptEntryExecutionHang(
            managedSqlScriptEntry.getScriptEntry(), managedScriptEntryStatus)
        && managedSqlScriptEntry.getManagedScriptStatus().getUpdatedAt() != null) {
      return Duration.between(Instant.parse(
              managedSqlScriptEntry.getManagedScriptStatus().getUpdatedAt()),
              Instant.now()).getSeconds() >= calculateExponentialBackoffDelay(
                  0, 600, 60, managedScriptEntryStatus.getIntents());
    }
    return false;
  }

  private boolean isSqlSameStatusHash(String sql) {
    return Objects.equals(
        ManagedSqlUtil.generateScriptEntryHash(managedSqlScriptEntry.getScriptEntry(), sql),
        managedSqlScriptEntry.getScriptEntryStatus().getHash());
  }

  private void executeScriptEntry(
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus,
      String sql) {
    try {
      setIntents(managedScriptEntryStatus);
      if (managedSqlScriptEntry.getManagedScriptStatus().getStartedAt() == null) {
        managedSqlScriptEntry.getManagedScriptStatus().setStartedAt(Instant.now().toString());
      }
      managedSqlScriptEntry.getManagedScriptStatus().setUpdatedAt(Instant.now().toString());
      managedSqlReconciliator.updateManagedSqlStatus(context,
          managedSqlScriptEntry.getManagedSqlStatus());
      managedSqlReconciliator.getManagedSqlScriptEntryExecutor()
          .executeScriptEntry(managedSqlScriptEntry, sql);
      resetIntentsAndFailure(managedScriptEntryStatus);
      if (Seq.seq(managedSqlScriptEntry.getScript().getSpec().getScripts()).findLast()
          .orElseThrow() == managedSqlScriptEntry.getScriptEntry()
          && managedSqlScriptEntry.getManagedScriptStatus().getScripts().stream()
          .map(StackGresClusterManagedScriptEntryScriptStatus::getFailureCode)
          .allMatch(Objects::isNull)) {
        managedSqlScriptEntry.getManagedScriptStatus().setFailedAt(null);
        managedSqlScriptEntry.getManagedScriptStatus().setCompletedAt(Instant.now().toString());
      }
    } catch (SQLException ex) {
      LOGGER.error("An error occurred while executing a managed script {}",
          managedSqlScriptEntry.getManagedScriptEntryDescription(), ex);
      setFailure(managedScriptEntryStatus, ex);
    } catch (Exception ex) {
      LOGGER.error("An error occurred while executing a managed script {}",
          managedSqlScriptEntry.getManagedScriptEntryDescription(), ex);
      setFailure(managedScriptEntryStatus, ex);
    }
  }

  private void setFailure(
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus, SQLException ex) {
    setFailure(managedScriptEntryStatus, ex, ex.getSQLState());
  }

  private void setFailure(
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus, Exception ex) {
    setFailure(managedScriptEntryStatus, ex, "XX500");
  }

  private void setFailure(
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus, Exception ex,
      String code) {
    managedScriptEntryStatus.setFailureCode(code);
    managedScriptEntryStatus.setFailure(ex.getMessage());
    managedSqlScriptEntry.getManagedScriptStatus().setFailedAt(Instant.now().toString());
  }

  private void setIntents(
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus) {
    managedScriptEntryStatus.setIntents(
        Optional.ofNullable(managedScriptEntryStatus.getIntents())
        .map(intents -> intents + 1)
        .orElse(1));
    managedScriptEntryStatus.setFailureCode(null);
    managedScriptEntryStatus.setFailure(null);
  }

  private void resetIntentsAndFailure(
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus) {
    managedScriptEntryStatus.setIntents(null);
    managedScriptEntryStatus.setFailureCode(null);
    managedScriptEntryStatus.setFailure(null);
  }

  protected ManagedSqlScriptEntry getManagedSqlScriptEntry() {
    return managedSqlScriptEntry;
  }

}
