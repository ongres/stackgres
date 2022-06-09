/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import static io.stackgres.common.RetryUtil.calculateExponentialBackoffDelay;

import java.sql.Connection;
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
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedSqlScriptEntryReconciliator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      ManagedSqlScriptEntryReconciliator.class);

  private final ManagedSqlReconciliator managedSqlReconciliator;
  private final KubernetesClient client;
  private final StackGresClusterContext context;
  private final StackGresClusterManagedSqlStatus managedSqlStatus;
  private final StackGresClusterManagedScriptEntry managedScript;
  private final StackGresClusterManagedScriptEntryStatus managedScriptStatus;
  private final StackGresScript script;
  private final StackGresScriptEntry scriptEntry;
  private final StackGresScriptEntryStatus scriptEntryStatus;

  protected ManagedSqlScriptEntryReconciliator(ManagedSqlReconciliator managedSqlReconciliator,
      KubernetesClient client, StackGresClusterContext context,
      StackGresClusterManagedSqlStatus managedSqlStatus,
      Tuple2<StackGresClusterManagedScriptEntry,
          StackGresClusterManagedScriptEntryStatus> managedScript,
      Tuple3<StackGresScript, StackGresScriptEntry, StackGresScriptEntryStatus> scriptEntry) {
    super();
    this.managedSqlReconciliator = managedSqlReconciliator;
    this.client = client;
    this.context = context;
    this.managedSqlStatus = managedSqlStatus;
    this.managedScript = managedScript.v1;
    this.managedScriptStatus = managedScript.v2;
    this.script = scriptEntry.v1;
    this.scriptEntry = scriptEntry.v2;
    this.scriptEntryStatus = scriptEntry.v3;
  }

  @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
      justification = "This is the feature not a bug")
  protected boolean reconcile() {
    final var managedScriptEntryStatus = getOrCreateScriptEntryStatus(
        managedScriptStatus, scriptEntry);
    managedScriptEntryStatus.setVersion(scriptEntry.getVersion());
    if (isExecutionBackOff(managedScriptStatus, managedScriptEntryStatus)) {
      LOGGER.warn("Back-off execution for managed script {}",
          getManagedScriptEntryDescription(managedScript, scriptEntry));
      return false;
    }
    final String sql = managedSqlReconciliator.getSql(context, scriptEntry);
    if (!isSqlSameStatusHash(scriptEntry, scriptEntryStatus, sql)) {
      LOGGER.warn("Skipping execution due to hash mismatch for managed script {}",
          getManagedScriptEntryDescription(managedScript, scriptEntry));
      return false;
    }
    executeScriptEntry(managedScriptStatus, managedScript,
        script, scriptEntry, managedScriptEntryStatus, sql);
    managedSqlReconciliator.updateManagedSqlStatus(context, managedSqlStatus);
    boolean isScriptEntryUpToDate = managedSqlReconciliator.isScriptEntryUpToDate(
        scriptEntry, managedScriptStatus);
    managedSqlReconciliator.sendEvent(client, context, managedScript, scriptEntry,
        managedScriptEntryStatus, isScriptEntryUpToDate);
    return isScriptEntryUpToDate;
  }

  private StackGresClusterManagedScriptEntryScriptStatus getOrCreateScriptEntryStatus(
      StackGresClusterManagedScriptEntryStatus managedScriptEntryStatus,
      StackGresScriptEntry scriptEntry) {
    var foundScriptEntryStatus = Optional.of(managedScriptEntryStatus)
        .map(StackGresClusterManagedScriptEntryStatus::getScripts)
        .stream().flatMap(List::stream)
        .filter(anScriptEntryStatus -> Objects.equals(
            scriptEntry.getId(), anScriptEntryStatus.getId()))
        .findFirst();
    final StackGresClusterManagedScriptEntryScriptStatus scriptEntryStatus;
    if (foundScriptEntryStatus.isPresent()) {
      scriptEntryStatus = foundScriptEntryStatus.get();
    } else {
      scriptEntryStatus = new StackGresClusterManagedScriptEntryScriptStatus();
      scriptEntryStatus.setId(scriptEntry.getId());
      if (managedScriptEntryStatus.getScripts() == null) {
        managedScriptEntryStatus.setScripts(new ArrayList<>());
      }
      managedScriptEntryStatus.getScripts().add(scriptEntryStatus);
    }
    return scriptEntryStatus;
  }

  private boolean isExecutionBackOff(StackGresClusterManagedScriptEntryStatus managedScriptStatus,
      final StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus) {
    return managedScriptStatus.getFailedAt() != null
        && managedScriptEntryStatus.getFailures() != null
        && Duration.between(Instant.parse(
            managedScriptStatus.getFailedAt()),
            Instant.now()).getSeconds() >= calculateExponentialBackoffDelay(
                0, 600, 60, managedScriptEntryStatus.getFailures());
  }

  private boolean isSqlSameStatusHash(StackGresScriptEntry scriptEntry,
      StackGresScriptEntryStatus scriptEntryStatus, String sql) {
    return Objects.equals(
        ManagedSqlUtil.generateScriptEntryHash(scriptEntry, sql),
        scriptEntryStatus.getHash());
  }

  private void executeScriptEntry(
      StackGresClusterManagedScriptEntryStatus managedScriptStatus,
      StackGresClusterManagedScriptEntry managedScript,
      StackGresScript script,
      StackGresScriptEntry scriptEntry,
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus,
      String sql) {
    if (managedScriptStatus.getStartedAt() == null) {
      managedScriptStatus.setStartedAt(Instant.now().toString());
    }
    try {
      executeScriptEntry(managedScript, scriptEntry, sql);
      managedScriptEntryStatus.setFailureCode(null);
      managedScriptEntryStatus.setFailure(null);
      if (Seq.seq(script.getSpec().getScripts()).findLast().orElseThrow() == scriptEntry
          && managedScriptStatus.getScripts().stream()
          .map(StackGresClusterManagedScriptEntryScriptStatus::getFailureCode)
          .allMatch(Objects::isNull)) {
        managedScriptStatus.setFailedAt(null);
        managedScriptStatus.setCompletedAt(Instant.now().toString());
      }
    } catch (SQLException ex) {
      LOGGER.error("An error occurred while executing a managed script {}",
          getManagedScriptEntryDescription(managedScript, scriptEntry), ex);
      setFailure(managedScriptStatus, managedScriptEntryStatus, ex);
    } catch (Exception ex) {
      LOGGER.error("An error occurred while executing a managed script {}",
          getManagedScriptEntryDescription(managedScript, scriptEntry), ex);
      setFailure(managedScriptStatus, managedScriptEntryStatus, ex);
    }
  }

  private void executeScriptEntry(
      StackGresClusterManagedScriptEntry managedScript,
      StackGresScriptEntry scriptEntry, String sql)
      throws SQLException {
    LOGGER.info("Executing managed script {}",
        getManagedScriptEntryDescription(managedScript, scriptEntry));
    try (Connection connection = managedSqlReconciliator.getConnection(
        scriptEntry.getDatabaseOrDefault(), scriptEntry.getUserOrDefault());
        var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private void setFailure(StackGresClusterManagedScriptEntryStatus managedScriptStatus,
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus, SQLException ex) {
    setFailure(managedScriptStatus, managedScriptEntryStatus, ex, ex.getSQLState());
  }

  private void setFailure(StackGresClusterManagedScriptEntryStatus managedScriptStatus,
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus, Exception ex) {
    setFailure(managedScriptStatus, managedScriptEntryStatus, ex, "XX500");
  }

  private void setFailure(StackGresClusterManagedScriptEntryStatus managedScriptStatus,
      StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus, Exception ex,
      String code) {
    managedScriptEntryStatus.setFailureCode(code);
    managedScriptEntryStatus.setFailure(ex.getMessage());
    managedScriptEntryStatus.setFailures(
        Optional.ofNullable(managedScriptEntryStatus.getFailures())
        .map(failures -> failures + 1)
        .orElse(1));
    managedScriptStatus.setFailedAt(Instant.now().toString());
  }

  private String getManagedScriptEntryDescription(StackGresClusterManagedScriptEntry managedScript,
      StackGresScriptEntry scriptEntry) {
    return managedScript.getId() + " (" + managedScript.getSgScript() + "),"
        + " entry " + scriptEntry.getId()
        + Optional.ofNullable(scriptEntry.getName())
            .map(name -> " (" + name + ")").orElse("");
  }

}
