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
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptStatusBuilder;
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
  private final String superuserUsername;

  protected ManagedSqlScriptEntryReconciliator(
      ManagedSqlReconciliator managedSqlReconciliator,
      KubernetesClient client,
      StackGresClusterContext context,
      ManagedSqlScriptEntry managedSqlScriptEntry,
      String superuserUsername) {
    super();
    this.managedSqlReconciliator = managedSqlReconciliator;
    this.client = client;
    this.context = context;
    this.managedSqlScriptEntry = managedSqlScriptEntry;
    this.superuserUsername = superuserUsername;
  }

  @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
      justification = "This is the feature not a bug")
  protected boolean reconcile() {
    if (managedSqlScriptEntry.getScriptEntry().getCron() != null) {
      return reconcileCron();
    }
    final var managedScriptEntryStatus = getOrCreateScriptEntryStatus();
    if ((managedSqlReconciliator.isScriptEntryExecutionHang(
        managedSqlScriptEntry.getScriptEntry(), managedScriptEntryStatus)
        || managedSqlReconciliator.isScriptEntryFailed(
            managedSqlScriptEntry.getScriptEntry(), managedScriptEntryStatus))
        && !managedSqlScriptEntry.getScriptEntry().getRetryOnErrorOrDefault()) {
      return false;
    }
    if (isExecutionBackOff(managedScriptEntryStatus)) {
      LOGGER.warn("Back-off execution for managed script {}",
          managedSqlScriptEntry.getManagedScriptEntryDescription());
      return false;
    }
    managedScriptEntryStatus.setVersion(managedSqlScriptEntry.getScriptEntry().getVersion());
    final String sql = managedSqlReconciliator.getSql(
        context, managedSqlScriptEntry.getScriptEntry());
    if (!isSqlSameStatusHash(sql)) {
      LOGGER.warn("Skipping execution due to hash mismatch for managed script {}",
          managedSqlScriptEntry.getManagedScriptEntryDescription());
      return false;
    }
    executeScriptEntry(managedScriptEntryStatus, sql, superuserUsername);
    managedSqlReconciliator.updateManagedSqlStatus(context,
        managedSqlScriptEntry.getManagedSqlStatus());
    boolean isScriptEntryUpToDate = managedSqlReconciliator.isScriptEntryUpToDate(
        managedSqlScriptEntry.getScriptEntry(), managedSqlScriptEntry.getManagedScriptStatus());
    managedSqlReconciliator.sendEvent(client, context,
        managedSqlScriptEntry.getManagedScript(), managedSqlScriptEntry.getScriptEntry(),
        managedScriptEntryStatus, isScriptEntryUpToDate);
    return isScriptEntryUpToDate;
  }

  /**
   * A script entry with a {@code cron} is executed each time it is due, regardless of
   * {@code retryOnError}, and the SGCluster status is only updated (and an event sent) when the
   * status of the entry changes (e.g. the value changed or the execution failed or recovered), so
   * that frequent schedules do not generate an update of the SGCluster on each execution.
   *
   * @return {@code true} if the current version of the script entry was executed successfully at
   *     least once, so that a failure of a scheduled execution does not block the execution of the
   *     following script entries.
   */
  @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
      justification = "This is the feature not a bug")
  private boolean reconcileCron() {
    final var managedScriptEntryStatus = getOrCreateScriptEntryStatus();
    final var previousManagedScriptEntryStatus =
        new StackGresClusterManagedScriptEntryScriptStatusBuilder(managedScriptEntryStatus)
        .build();
    final String sql = managedSqlReconciliator.getSql(
        context, managedSqlScriptEntry.getScriptEntry());
    if (!isSqlSameStatusHash(sql)) {
      LOGGER.warn("Skipping execution due to hash mismatch for managed script {}",
          managedSqlScriptEntry.getManagedScriptEntryDescription());
      return false;
    }
    final boolean failedBefore = managedScriptEntryStatus.getFailureCode() != null;
    // Scheduled executions are only logged the first time, when they fail or when they recover
    if (managedSqlReconciliator.getCronScheduler().isFirstExecution(
        managedSqlScriptEntry.getManagedScript(), managedSqlScriptEntry.getScriptEntry())) {
      LOGGER.info("Executing managed script {} that will be re-executed following the schedule {}"
          + " (further executions will only be logged if they fail)",
          managedSqlScriptEntry.getManagedScriptEntryDescription(),
          managedSqlReconciliator.getCronScheduler().getCadence(
              managedSqlScriptEntry.getScriptEntry()));
    }
    Exception failure = null;
    try {
      final Optional<String> value = managedSqlReconciliator.getManagedSqlScriptEntryExecutor()
          .executeScriptEntry(managedSqlScriptEntry, sql, superuserUsername);
      managedScriptEntryStatus.setVersion(managedSqlScriptEntry.getScriptEntry().getVersion());
      resetIntentsAndFailure(managedScriptEntryStatus);
      if (managedSqlScriptEntry.getScriptEntry().getSetValueOrDefault()) {
        managedScriptEntryStatus.setValue(value.orElse(null));
      }
    } catch (Exception ex) {
      failure = ex;
      managedScriptEntryStatus.setIntents(1);
      managedScriptEntryStatus.setFailureCode(
          ex instanceof SQLException sqlException ? sqlException.getSQLState() : "XX500");
      managedScriptEntryStatus.setFailure(ex.getMessage());
    } finally {
      managedSqlReconciliator.getCronScheduler().executed(
          managedSqlScriptEntry.getManagedScript(), managedSqlScriptEntry.getScriptEntry());
    }
    final boolean failed = managedScriptEntryStatus.getFailureCode() != null;
    final boolean changed =
        !Objects.equals(previousManagedScriptEntryStatus, managedScriptEntryStatus);
    if (failure != null && changed) {
      LOGGER.error("An error occurred while executing a managed script {}",
          managedSqlScriptEntry.getManagedScriptEntryDescription(), failure);
    } else if (failure != null) {
      LOGGER.debug("The managed script {} failed again: {}",
          managedSqlScriptEntry.getManagedScriptEntryDescription(), failure.getMessage());
    } else if (failedBefore) {
      LOGGER.info("The managed script {} was executed successfully after failing",
          managedSqlScriptEntry.getManagedScriptEntryDescription());
    }
    if (changed) {
      final String now = Instant.now().toString();
      final var managedScriptStatus = managedSqlScriptEntry.getManagedScriptStatus();
      if (managedScriptStatus.getStartedAt() == null) {
        managedScriptStatus.setStartedAt(now);
      }
      managedScriptStatus.setUpdatedAt(now);
      if (failed) {
        managedScriptStatus.setFailedAt(now);
      } else if (failedBefore
          && managedScriptStatus.getScripts().stream()
          .map(StackGresClusterManagedScriptEntryScriptStatus::getFailureCode)
          .allMatch(Objects::isNull)) {
        managedScriptStatus.setFailedAt(null);
        managedScriptStatus.setCompletedAt(now);
      }
      managedSqlReconciliator.updateManagedSqlStatus(context,
          managedSqlScriptEntry.getManagedSqlStatus());
      if (failed != failedBefore || failed || previousManagedScriptEntryStatus.getVersion() == null) {
        managedSqlReconciliator.sendEvent(client, context,
            managedSqlScriptEntry.getManagedScript(), managedSqlScriptEntry.getScriptEntry(),
            managedScriptEntryStatus, !failed);
      }
    }
    return Objects.equals(
        managedScriptEntryStatus.getVersion(), managedSqlScriptEntry.getScriptEntry().getVersion());
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
              Instant.now()).getSeconds() < calculateExponentialBackoffDelay(
                  10, 600, 10, managedScriptEntryStatus.getIntents());
    }
    if (managedSqlReconciliator.isScriptEntryExecutionHang(
            managedSqlScriptEntry.getScriptEntry(), managedScriptEntryStatus)
        && managedSqlScriptEntry.getManagedScriptStatus().getUpdatedAt() != null) {
      return Duration.between(Instant.parse(
              managedSqlScriptEntry.getManagedScriptStatus().getUpdatedAt()),
              Instant.now()).getSeconds() < calculateExponentialBackoffDelay(
                  10, 600, 10, managedScriptEntryStatus.getIntents());
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
      String sql,
      String superuserUsername) {
    try {
      setIntents(managedScriptEntryStatus);
      if (managedSqlScriptEntry.getManagedScriptStatus().getStartedAt() == null) {
        managedSqlScriptEntry.getManagedScriptStatus().setStartedAt(Instant.now().toString());
      }
      managedSqlScriptEntry.getManagedScriptStatus().setUpdatedAt(Instant.now().toString());
      managedSqlReconciliator.updateManagedSqlStatus(context,
          managedSqlScriptEntry.getManagedSqlStatus());
      final Optional<String> value = managedSqlReconciliator.getManagedSqlScriptEntryExecutor()
          .executeScriptEntry(managedSqlScriptEntry, sql, superuserUsername);
      resetIntentsAndFailure(managedScriptEntryStatus);
      if (managedSqlScriptEntry.getScriptEntry().getSetValueOrDefault()) {
        managedScriptEntryStatus.setValue(value.orElse(null));
      }
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
