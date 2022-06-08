/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.RemoveOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ScriptsConfigMutator
    implements ClusterMutator {

  private static final long VERSION_1_2 = StackGresVersion.V_1_2.getVersionAsNumber();

  static final JsonPointer INITIAL_DATA_SCRIPTS_POINTER = SPEC_POINTER.append("initialData")
      .append("scripts");
  static final JsonPointer MANAGED_SQL_POINTER = SPEC_POINTER.append("managedSql");
  static final JsonPointer MANAGED_SQL_SCRIPTS_POINTER = MANAGED_SQL_POINTER.append("scripts");
  static final JsonPointer MANAGED_SQL_STATUS_POINTER = STATUS_POINTER.append("managedSql");

  private final CustomResourceScheduler<StackGresScript> scriptScheduler;

  @Inject
  public ScriptsConfigMutator(CustomResourceScheduler<StackGresScript> scriptScheduler) {
    this.scriptScheduler = scriptScheduler;
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    AdmissionRequest<StackGresCluster> request = review.getRequest();
    ImmutableList.Builder<JsonPatchOperation> builder = ImmutableList.builder();
    final var managedSql = request.getObject().getSpec().getManagedSql();
    final var status = request.getObject().getStatus();
    final var managedSqlStatus = Optional.ofNullable(status)
        .map(StackGresClusterStatus::getManagedSql)
        .orElse(null);
    final boolean addDefaultScripts = addDefaultScripts(request, builder);
    final boolean fillRequiredFields = fillRequiredFields(request);
    if (addDefaultScripts || fillRequiredFields) {
      if (managedSql == null) {
        builder.add(new AddOperation(MANAGED_SQL_POINTER,
            FACTORY.pojoNode(request.getObject().getSpec().getManagedSql())));
      } else {
        builder.add(new ReplaceOperation(MANAGED_SQL_POINTER,
            FACTORY.pojoNode(request.getObject().getSpec().getManagedSql())));
      }
    }
    final boolean updateScriptsStatuses = updateScriptsStatuses(request);
    if (updateScriptsStatuses) {
      if (status != null) {
        if (managedSqlStatus != null) {
          builder.add(new ReplaceOperation(MANAGED_SQL_STATUS_POINTER,
              FACTORY.pojoNode(request.getObject().getStatus().getManagedSql())));
        } else {
          builder.add(new AddOperation(MANAGED_SQL_STATUS_POINTER,
              FACTORY.pojoNode(request.getObject().getStatus().getManagedSql())));
        }
      } else {
        builder.add(new AddOperation(STATUS_POINTER,
            FACTORY.pojoNode(request.getObject().getStatus())));
      }
    }
    return builder.build();
  }

  private boolean addDefaultScripts(AdmissionRequest<StackGresCluster> request,
      ImmutableList.Builder<JsonPatchOperation> builder) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(request.getObject());
    if (version <= VERSION_1_2
        && (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE)) {
      addDefaultScript(request);
      if (moveInitialDataScripts(request)) {
        builder.add(new RemoveOperation(INITIAL_DATA_SCRIPTS_POINTER));
      }
      return true;
    }
    if (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE) {
      if (addDefaultScript(request)) {
        return true;
      }
    }
    return false;
  }

  private boolean addDefaultScript(AdmissionRequest<StackGresCluster> request) {
    if (request.getObject().getSpec().getManagedSql() == null) {
      request.getObject().getSpec().setManagedSql(new StackGresClusterManagedSql());
    }
    if (request.getObject().getSpec().getManagedSql().getScripts() == null) {
      request.getObject().getSpec().getManagedSql().setScripts(new ArrayList<>());
    }
    String defaultScriptName = ManagedSqlUtil.defaultName(request.getObject());
    if (request.getObject().getSpec().getManagedSql().getScripts().stream()
        .map(StackGresClusterManagedScriptEntry::getSgScript).anyMatch(defaultScriptName::equals)) {
      return false;
    }
    request.getObject().getSpec().getManagedSql().getScripts()
        .add(0, new StackGresClusterManagedScriptEntry());
    request.getObject().getSpec().getManagedSql().getScripts().get(0).setSgScript(
        defaultScriptName);
    return true;
  }

  private boolean moveInitialDataScripts(AdmissionRequest<StackGresCluster> request) {
    if (request.getObject().getSpec().getInitData() == null
        || request.getObject().getSpec().getInitData().getScripts() == null
        || request.getObject().getSpec().getInitData().getScripts().isEmpty()) {
      return false;
    }
    request.getObject().getSpec().getManagedSql().getScripts()
        .add(1, new StackGresClusterManagedScriptEntry());
    request.getObject().getSpec().getManagedSql().getScripts().get(0).setSgScript(
        ManagedSqlUtil.initialDataName(request.getObject()));
    StackGresScript initDataScript = new StackGresScript();
    initDataScript.setSpec(new StackGresScriptSpec());
    initDataScript.getSpec().setScripts(new ArrayList<>());
    request.getObject().getSpec().getInitData().getScripts().forEach(initDataScriptEntry -> {
      StackGresScriptEntry initDataScriptEntryCopy = new StackGresScriptEntry();
      initDataScriptEntryCopy.setName(initDataScriptEntry.getName());
      initDataScriptEntryCopy.setDatabase(initDataScriptEntry.getDatabase());
      if (initDataScriptEntry.getScript() != null) {
        initDataScriptEntryCopy.setScript(initDataScriptEntry.getScript());
      } else {
        initDataScriptEntryCopy.setScriptFrom(new StackGresScriptFrom());
        if (initDataScriptEntry.getScriptFrom().getConfigMapKeyRef() != null) {
          initDataScriptEntryCopy.getScriptFrom()
              .setConfigMapKeyRef(new ConfigMapKeySelector());
          initDataScriptEntryCopy.getScriptFrom().getConfigMapKeyRef().setName(
              initDataScriptEntry.getScriptFrom().getConfigMapKeyRef().getName());
          initDataScriptEntryCopy.getScriptFrom().getConfigMapKeyRef().setKey(
              initDataScriptEntry.getScriptFrom().getConfigMapKeyRef().getKey());
        } else {
          initDataScriptEntryCopy.getScriptFrom()
              .setSecretKeyRef(new SecretKeySelector());
          initDataScriptEntryCopy.getScriptFrom().getSecretKeyRef().setName(
              initDataScriptEntry.getScriptFrom().getSecretKeyRef().getName());
          initDataScriptEntryCopy.getScriptFrom().getSecretKeyRef().setKey(
              initDataScriptEntry.getScriptFrom().getSecretKeyRef().getKey());
        }
      }
      initDataScript.getSpec().getScripts().add(initDataScriptEntryCopy);
    });
    scriptScheduler.create(initDataScript);
    return true;
  }

  private boolean fillRequiredFields(AdmissionRequest<StackGresCluster> request) {
    if (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE) {
      boolean result = false;
      int lastId = Optional.of(request.getObject())
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getManagedSql)
          .map(StackGresClusterManagedSql::getScripts)
          .stream()
          .flatMap(List::stream)
          .map(StackGresClusterManagedScriptEntry::getId)
          .reduce(-1, (last, id) -> id == null || last >= id ? last : id, (u, v) -> v);
      for (StackGresClusterManagedScriptEntry scriptEntry : Optional.of(request.getObject())
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getManagedSql)
          .map(StackGresClusterManagedSql::getScripts)
          .orElse(List.of())) {
        if (scriptEntry.getId() == null) {
          lastId++;
          scriptEntry.setId(lastId);
          result = true;
        }
      }
      return result;
    }
    return false;
  }

  private boolean updateScriptsStatuses(
      AdmissionRequest<StackGresCluster> request) {
    if (request.getObject().getStatus() == null) {
      request.getObject().setStatus(new StackGresClusterStatus());
    }
    if (request.getObject().getStatus().getManagedSql() == null) {
      request.getObject().getStatus().setManagedSql(new StackGresClusterManagedSqlStatus());
    }
    if (request.getObject().getStatus().getManagedSql().getScripts() == null) {
      request.getObject().getStatus().getManagedSql().setScripts(new ArrayList<>());
    }
    List<StackGresClusterManagedScriptEntryStatus> scriptsStatuses =
        request.getObject().getStatus().getManagedSql().getScripts();
    var scriptsToAdd = Optional.of(request.getObject())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .filter(script -> scriptsStatuses.stream()
            .noneMatch(statusScript -> Objects.equals(
                statusScript.getId(), script.getId())))
        .toList();
    scriptsToAdd.forEach(scriptEntry -> addScriptEntryStatus(
            scriptEntry, scriptsStatuses));
    var scriptsToRemove = Seq.seq(scriptsStatuses)
        .zipWithIndex()
        .filter(scriptStatus -> Optional.of(request.getObject())
            .map(StackGresCluster::getSpec)
            .map(StackGresClusterSpec::getManagedSql)
            .map(StackGresClusterManagedSql::getScripts)
            .stream()
            .flatMap(List::stream)
            .noneMatch(scriptEntry -> Objects
                .equals(scriptStatus.v1.getId(), scriptEntry.getId())))
        .reverse()
        .toList();
    scriptsToRemove.forEach(tuple -> scriptsStatuses.remove(tuple.v2.intValue()));
    return !scriptsToAdd.isEmpty() || !scriptsToRemove.isEmpty();
  }

  private void addScriptEntryStatus(StackGresClusterManagedScriptEntry scriptEntry,
      List<StackGresClusterManagedScriptEntryStatus> scriptsStatuses) {
    StackGresClusterManagedScriptEntryStatus scriptEntryStatus =
        new StackGresClusterManagedScriptEntryStatus();
    scriptEntryStatus.setId(scriptEntry.getId());
    scriptsStatuses.add(scriptEntryStatus);
  }

}
