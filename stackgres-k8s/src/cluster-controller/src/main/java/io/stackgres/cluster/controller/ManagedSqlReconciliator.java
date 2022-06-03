/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.ClusterControllerEventReason;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.cluster.configuration.ClusterControllerPropertyContext;
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;
import io.stackgres.common.postgres.PostgresConnectionManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ManagedSqlReconciliator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagedSqlReconciliator.class);

  private final boolean reconcileManagedSql;
  private final ResourceFinder<Endpoints> endpointsFinder;
  private final CustomResourceFinder<StackGresScript> scriptFinder;
  private final ResourceFinder<Secret> secretFinder;
  private final ResourceFinder<ConfigMap> configMapFinder;
  private final PostgresConnectionManager postgresConnectionManager;
  private final CustomResourceScheduler<StackGresCluster> clusterScheduler;
  private final String podName;
  private final EventController eventController;

  @Dependent
  public static class Parameters {
    @Inject ClusterControllerPropertyContext propertyContext;
    @Inject ResourceFinder<Endpoints> endpointsFinder;
    @Inject CustomResourceFinder<StackGresScript> scriptFinder;
    @Inject ResourceFinder<Secret> secretFinder;
    @Inject ResourceFinder<ConfigMap> configMapFinder;
    @Inject PostgresConnectionManager postgresConnectionManager;
    @Inject CustomResourceScheduler<StackGresCluster> clusterScheduler;
    @Inject EventController eventController;
  }

  @Inject
  public ManagedSqlReconciliator(Parameters parameters) {
    this.reconcileManagedSql = parameters.propertyContext
        .getBoolean(ClusterControllerProperty.CLUSTER_CONTROLLER_RECONCILE_MANAGED_SQL);
    this.endpointsFinder = parameters.endpointsFinder;
    this.scriptFinder = parameters.scriptFinder;
    this.secretFinder = parameters.secretFinder;
    this.configMapFinder = parameters.configMapFinder;
    this.postgresConnectionManager = parameters.postgresConnectionManager;
    this.clusterScheduler = parameters.clusterScheduler;
    this.podName = parameters.propertyContext
        .getString(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME);
    this.eventController = parameters.eventController;
  }

  public ReconciliationResult<Boolean> reconcile(KubernetesClient client,
      StackGresClusterContext context) {
    StackGresClusterManagedSqlStatus managedSqlStatus = getManagedSqlStatus(context);
    if (!reconcileManagedSql || managedSqlStatus == null) {
      return new ReconciliationResult<>(false);
    }
    try {
      reconcileManagedSql(client, context, managedSqlStatus);
      return new ReconciliationResult<>(false);
    } catch (Exception ex) {
      try {
        eventController.sendEvent(ClusterControllerEventReason.CLUSTER_CONTROLLER_ERROR,
            "An error occurred while reconciling managed SQL configuration: " + ex.getMessage(),
            client);
      } catch (Exception eventEx) {
        LOGGER.error("An error occurred while sending an event", eventEx);
      }
      return new ReconciliationResult<>(false, ex);
    }
  }

  private StackGresClusterManagedSqlStatus getManagedSqlStatus(StackGresClusterContext context) {
    var managedSqlStatus = Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getManagedSql)
        .orElse(null);
    if (managedSqlStatus != null && managedSqlStatus.getScripts() == null) {
      managedSqlStatus.setScripts(new ArrayList<>());
    }
    return managedSqlStatus;
  }

  private void reconcileManagedSql(
      KubernetesClient client,
      StackGresClusterContext context,
      StackGresClusterManagedSqlStatus managedSqlStatus) {
    if (Optional.of(context.getCluster().getSpec())
        .map(StackGresClusterSpec::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .count() == 0
        || !isBootstrappedLeader(context)) {
      return;
    }
    var scriptsStatus = Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getManagedSql)
        .map(StackGresClusterManagedSqlStatus::getScripts)
        .orElse(new ArrayList<>());
    var managedScripts = Optional.of(context.getCluster().getSpec())
        .map(StackGresClusterSpec::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .map(Tuple::tuple)
        .map(t -> t.concat(findManagedScript(context, t.v1)))
        .map(t -> t.concat(findManagedScriptStatus(scriptsStatus, t.v1)))
        .toList();
    for (var managedScript : managedScripts) {
      var managedScriptEntries = Optional.of(managedScript.v2.getSpec())
          .map(StackGresScriptSpec::getScripts)
          .stream()
          .flatMap(List::stream)
          .map(managedScript::concat)
          .filter(t -> !isScriptEntryUpToDate(t.v4, t.v3))
          .map(t -> t.concat(findScriptStatus(t.v1.getId(), t.v2, t.v4)))
          .toList();
      boolean scriptResult = true;
      for (var managedScriptEntry : managedScriptEntries) {
        boolean result = safeManageScriptEntry(client, context, managedSqlStatus,
            managedScriptEntry.v1, managedScriptEntry.v3,
            managedScriptEntry.v2, managedScriptEntry.v4, managedScriptEntry.v5);
        scriptResult = scriptResult && result;
        if (!result && !doesScriptEntryContinueOnError(managedScriptEntry.v2)) {
          break;
        }
      }
      if (!scriptResult && !doesManagedScriptContinueOnError(context)) {
        break;
      }
    }
  }

  private boolean doesScriptEntryContinueOnError(StackGresScript script) {
    return Optional.ofNullable(script)
    .map(StackGresScript::getSpec)
    .map(StackGresScriptSpec::isContinueOnError)
    .orElse(false);
  }

  private boolean doesManagedScriptContinueOnError(StackGresClusterContext context) {
    return Optional.ofNullable(context.getCluster().getSpec()
        .getManagedSql().getContinueOnScriptError())
    .orElse(false);
  }

  private boolean isBootstrappedLeader(StackGresClusterContext context) {
    Optional<Endpoints> patroniEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.name(context.getCluster()),
            context.getCluster().getMetadata().getNamespace());
    Optional<Endpoints> patroniConfigEndpoints = endpointsFinder
        .findByNameAndNamespace(PatroniUtil.configName(context.getCluster()),
            context.getCluster().getMetadata().getNamespace());
    return patroniEndpoints.map(Endpoints::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(PatroniUtil.LEADER_KEY))
        .map(this.podName::equals).orElse(false)
        && patroniConfigEndpoints.map(Endpoints::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(PatroniUtil.INITIALIZE_KEY))
        .isPresent();
  }

  private StackGresScript findManagedScript(StackGresClusterContext context,
      StackGresClusterManagedScriptEntry managedScript) {
    return scriptFinder
        .findByNameAndNamespace(
            managedScript.getSgScript(),
            context.getCluster().getMetadata().getNamespace())
        .orElseThrow(() -> new IllegalArgumentException(
            "Managed script " + managedScript.getId() + "(" + managedScript.getSgScript()
                + ") was not found"));
  }

  private StackGresClusterManagedScriptEntryStatus findManagedScriptStatus(
      List<StackGresClusterManagedScriptEntryStatus> scriptsStatus,
      StackGresClusterManagedScriptEntry managedScript) {
    return scriptsStatus.stream()
          .filter(aManagedScriptStatus -> Objects.equals(
              managedScript.getId(), aManagedScriptStatus.getId()))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(
              "Status for managed script " + managedScript.getId() + "("
                  + managedScript.getSgScript() + ") was not found"));
  }

  private StackGresScriptEntryStatus findScriptStatus(
      Integer managedScriptId,
      StackGresScript script, StackGresScriptEntry scriptEntry) {
    return Optional.of(script)
        .map(StackGresScript::getStatus)
        .map(StackGresScriptStatus::getScripts)
        .stream()
        .flatMap(List::stream)
        .filter(aScriptStatus -> Objects.equals(
            scriptEntry.getId(), aScriptStatus.getId()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Status for entry " + scriptEntry.getId() + " of managed script "
                + managedScriptId + " (" + script.getMetadata().getName() + ")"
                + " was not found"));
  }

  private boolean isScriptEntryUpToDate(StackGresScriptEntry scriptEntry,
      StackGresClusterManagedScriptEntryStatus managedScriptStatus) {
    return Optional.of(managedScriptStatus)
        .map(StackGresClusterManagedScriptEntryStatus::getScripts)
        .stream().flatMap(List::stream)
        .filter(aScriptEntryStatus -> Objects.equals(
            scriptEntry.getId(), aScriptEntryStatus.getId()))
        .anyMatch(scriptEntryStatus -> isScriptEntryUpToDate(scriptEntry, scriptEntryStatus));
  }

  private boolean isScriptEntryUpToDate(StackGresScriptEntry scriptEntry,
      StackGresClusterManagedScriptEntryScriptStatus mangedScriptStatus) {
    return mangedScriptStatus.getFailureCode() == null
        && Objects.equals(scriptEntry.getVersion(), mangedScriptStatus.getVersion());
  }

  private boolean safeManageScriptEntry(KubernetesClient client, StackGresClusterContext context,
      StackGresClusterManagedSqlStatus managedSqlStatus,
      StackGresClusterManagedScriptEntry managedScript,
      StackGresClusterManagedScriptEntryStatus managedScriptStatus,
      StackGresScript script,
      StackGresScriptEntry scriptEntry,
      StackGresScriptEntryStatus scriptEntryStatus) {
    final var managedScriptEntryStatus = getOrCreateScriptEntryStatus(
        managedScriptStatus, scriptEntry);
    managedScriptEntryStatus.setVersion(scriptEntry.getVersion());
    final String sql = getSql(context, scriptEntry);
    if (!Objects.equals(
        ManagedSqlUtil.generateScriptEntryHash(scriptEntry, sql),
        scriptEntryStatus.getHash())) {
      LOGGER.warn("Skipping execution due to hash mismatch for managed script {}",
          getManagedScriptEntryDescription(managedScript, scriptEntry));
      return false;
    }
    executeScriptEntry(context, managedScriptStatus, managedScript,
        script, scriptEntry, managedScriptEntryStatus, sql);
    clusterScheduler.updateStatus(context.getCluster(),
        StackGresCluster::getStatus,
        (targetCluster, status) -> targetCluster.getStatus().setManagedSql(managedSqlStatus));
    boolean isScriptEntryUpToDate = isScriptEntryUpToDate(scriptEntry, managedScriptStatus);
    sendEvent(client, context, managedScript, scriptEntry, managedScriptEntryStatus,
        isScriptEntryUpToDate);
    return isScriptEntryUpToDate;
  }

  private StackGresClusterManagedScriptEntryScriptStatus getOrCreateScriptEntryStatus(
      StackGresClusterManagedScriptEntryStatus managedScriptEntryStatus,
      StackGresScriptEntry scriptEntry) {
    var foundScriptEntryStatus = Optional.of(managedScriptEntryStatus)
        .map(StackGresClusterManagedScriptEntryStatus::getScripts)
        .stream().flatMap(List::stream)
        .filter(aScriptEntryStatus -> Objects.equals(
            scriptEntry.getId(), aScriptEntryStatus.getId()))
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

  private void executeScriptEntry(StackGresClusterContext context,
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
      executeScriptEntry(context, scriptEntry, sql);
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
      managedScriptEntryStatus.setFailureCode(ex.getSQLState());
      managedScriptEntryStatus.setFailure(ex.getMessage());
      managedScriptStatus.setFailedAt(Instant.now().toString());
    } catch (Exception ex) {
      LOGGER.error("An error occurred while executing a managed script {}",
          getManagedScriptEntryDescription(managedScript, scriptEntry), ex);
      managedScriptEntryStatus.setFailureCode("XX500");
      managedScriptEntryStatus.setFailure(ex.getMessage());
      managedScriptStatus.setFailedAt(Instant.now().toString());
    }
  }

  private void executeScriptEntry(StackGresClusterContext context, StackGresScriptEntry scriptEntry,
      String sql) throws SQLException {
    try (Connection connection = getConnection(
        scriptEntry.getDatabaseOrDefault(), scriptEntry.getUserOrDefault());
        var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private String getSql(StackGresClusterContext context, StackGresScriptEntry scriptEntry) {
    final String sql;
    if (scriptEntry.getScript() != null) {
      sql = scriptEntry.getScript();
    } else {
      if (scriptEntry.getScriptFrom().getConfigMapKeyRef() != null) {
        var configMapKeyRef = scriptEntry.getScriptFrom().getConfigMapKeyRef();
        sql = Optional.of(configMapFinder.findByNameAndNamespace(
            configMapKeyRef.getName(),
            context.getCluster().getMetadata().getNamespace())
            .orElseThrow(() -> new IllegalArgumentException(
                "ConfigMap " + configMapKeyRef.getName() + " not found")))
            .map(ConfigMap::getData)
            .map(data -> data.get(configMapKeyRef.getKey()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Key " + configMapKeyRef.getKey() + " not found in ConfigMap "
                    + configMapKeyRef.getName()));
      } else {
        var secretKeyRef = scriptEntry.getScriptFrom().getSecretKeyRef();
        sql = Optional.of(secretFinder.findByNameAndNamespace(
            secretKeyRef.getName(),
            context.getCluster().getMetadata().getNamespace())
            .orElseThrow(() -> new IllegalArgumentException(
                "Secret " + secretKeyRef.getName() + " not found")))
            .map(Secret::getData)
            .map(data -> data.get(secretKeyRef.getKey()))
            .map(ResourceUtil::dencodeSecret)
            .orElseThrow(() -> new IllegalArgumentException(
                "Key " + secretKeyRef.getKey() + " not found in Secret "
                    + secretKeyRef.getName()));
      }
    }
    return sql;
  }

  private void sendEvent(KubernetesClient client, StackGresClusterContext context,
      StackGresClusterManagedScriptEntry managedScript, StackGresScriptEntry scriptEntry,
      final StackGresClusterManagedScriptEntryScriptStatus managedScriptEntryStatus,
      boolean isScriptEntryUpToDate) {
    if (isScriptEntryUpToDate) {
      eventController.sendEvent(ClusterControllerEventReason.CLUSTER_MANAGED_SQL,
          "Managed script " + getManagedScriptEntryDescription(managedScript, scriptEntry)
              + " has been executed successfully", context.getCluster(), client);
    } else {
      eventController.sendEvent(ClusterControllerEventReason.CLUSTER_MANAGED_SQL_ERROR,
          "Managed script " + getManagedScriptEntryDescription(managedScript, scriptEntry)
              + " has failed (" + managedScriptEntryStatus.getFailureCode() + "): "
              + managedScriptEntryStatus.getFailure(), context.getCluster(), client);
    }
  }

  private String getManagedScriptEntryDescription(StackGresClusterManagedScriptEntry managedScript,
      StackGresScriptEntry scriptEntry) {
    return managedScript.getId() + " (" + managedScript.getSgScript() + "),"
        + " entry " + scriptEntry.getId()
        + Optional.ofNullable(scriptEntry.getName())
            .map(name -> " (" + name + ")").orElse("");
  }

  private Connection getConnection(String database, String user)
      throws SQLException {
    return postgresConnectionManager.getConnection(
        ClusterStatefulSetPath.PG_DATA_PATH.path(), EnvoyUtil.PG_PORT,
        database,
        user,
        "");
  }

}
