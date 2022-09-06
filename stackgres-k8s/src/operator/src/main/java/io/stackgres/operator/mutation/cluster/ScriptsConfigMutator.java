/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.time.Instant;
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
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.StackGresVersion;
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
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ScriptsConfigMutator
    implements ClusterMutator {

  private static final int DEFAULT_SCRIPT_ID = 0;
  private static final int DEFAULT_SCRIPT_INDEX = 0;
  private static final int INITIAL_DATA_SCRIPT_ID = 1;
  private static final int INITIAL_DATA_SCRIPT_INDEX = 1;

  private static final long VERSION_1_2 = StackGresVersion.V_1_2.getVersionAsNumber();

  static final JsonPointer INITIAL_DATA_SCRIPTS_POINTER = SPEC_POINTER.append("initialData")
      .append("scripts");
  static final JsonPointer MANAGED_SQL_POINTER = SPEC_POINTER.append("managedSql");
  static final JsonPointer MANAGED_SQL_SCRIPTS_POINTER = MANAGED_SQL_POINTER.append("scripts");
  static final JsonPointer MANAGED_SQL_STATUS_POINTER = STATUS_POINTER.append("managedSql");

  private final CustomResourceFinder<StackGresScript> scriptFinder;
  private final CustomResourceScheduler<StackGresScript> scriptScheduler;

  @Inject
  public ScriptsConfigMutator(CustomResourceFinder<StackGresScript> scriptFinder,
      CustomResourceScheduler<StackGresScript> scriptScheduler) {
    this.scriptFinder = scriptFinder;
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
    final boolean addDefaultScripts = addDefaultScripts(request);
    final boolean moveInitialDataScripts = moveInitialDataScripts(request);
    resetIdForScriptsWithDefaultId(request);
    final boolean fillRequiredFields = fillRequiredFields(request);
    if (addDefaultScripts || moveInitialDataScripts || fillRequiredFields) {
      if (managedSql == null) {
        builder.add(new AddOperation(MANAGED_SQL_POINTER,
            FACTORY.pojoNode(request.getObject().getSpec().getManagedSql())));
      } else {
        builder.add(new ReplaceOperation(MANAGED_SQL_POINTER,
            FACTORY.pojoNode(request.getObject().getSpec().getManagedSql())));
      }
    }
    if (moveInitialDataScripts) {
      builder.add(new RemoveOperation(INITIAL_DATA_SCRIPTS_POINTER));
    }
    final boolean updateScriptsStatuses = updateScriptsStatuses(request);
    if (updateScriptsStatuses || addDefaultScripts || moveInitialDataScripts) {
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

  private boolean addDefaultScripts(AdmissionRequest<StackGresCluster> request) {
    if (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE) {
      if (request.getObject().getSpec().getManagedSql() == null) {
        request.getObject().getSpec().setManagedSql(new StackGresClusterManagedSql());
      }
      if (request.getObject().getSpec().getManagedSql().getScripts() == null) {
        request.getObject().getSpec().getManagedSql().setScripts(new ArrayList<>());
      }
      String defaultScriptName = ManagedSqlUtil.defaultName(request.getObject());
      if (request.getObject().getSpec().getManagedSql().getScripts().stream()
          .map(StackGresClusterManagedScriptEntry::getSgScript)
          .anyMatch(defaultScriptName::equals)) {
        return false;
      }
      request.getObject().getSpec().getManagedSql().getScripts()
          .add(DEFAULT_SCRIPT_INDEX, new StackGresClusterManagedScriptEntry());
      request.getObject().getSpec().getManagedSql().getScripts().get(DEFAULT_SCRIPT_INDEX)
          .setId(DEFAULT_SCRIPT_ID);
      request.getObject().getSpec().getManagedSql().getScripts().get(DEFAULT_SCRIPT_INDEX)
          .setSgScript(defaultScriptName);
      if (request.getObject().getStatus() == null) {
        request.getObject().setStatus(new StackGresClusterStatus());
      }
      if (request.getObject().getStatus().getManagedSql() == null) {
        request.getObject().getStatus().setManagedSql(new StackGresClusterManagedSqlStatus());
      }
      if (request.getObject().getStatus().getManagedSql().getScripts() == null) {
        request.getObject().getStatus().getManagedSql().setScripts(new ArrayList<>());
      }
      request.getObject().getStatus().getManagedSql().getScripts()
          .removeIf(entry -> Objects.equals(DEFAULT_SCRIPT_ID, entry.getId()));
      request.getObject().getStatus().getManagedSql().getScripts()
          .add(DEFAULT_SCRIPT_INDEX, new StackGresClusterManagedScriptEntryStatus());
      request.getObject().getStatus().getManagedSql().getScripts().get(DEFAULT_SCRIPT_INDEX)
          .setId(DEFAULT_SCRIPT_ID);
      return true;
    }
    return false;
  }

  private boolean moveInitialDataScripts(AdmissionRequest<StackGresCluster> request) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(request.getObject());
    if (version <= VERSION_1_2
        && (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE)) {
      if (request.getObject().getSpec().getInitData() == null
          || request.getObject().getSpec().getInitData().getScripts() == null
          || request.getObject().getSpec().getInitData().getScripts().isEmpty()) {
        return false;
      }
      StackGresScript initDataScript = createInitDataScript(request);
      if (scriptFinder.findByNameAndNamespace(
          initDataScript.getMetadata().getName(),
          initDataScript.getMetadata().getNamespace()).isEmpty()) {
        scriptScheduler.create(initDataScript);
      } else {
        scriptScheduler.update(initDataScript);
      }
      request.getObject().getSpec().getManagedSql().getScripts()
          .add(INITIAL_DATA_SCRIPT_INDEX, new StackGresClusterManagedScriptEntry());
      request.getObject().getSpec().getManagedSql().getScripts().get(INITIAL_DATA_SCRIPT_INDEX)
          .setId(INITIAL_DATA_SCRIPT_ID);
      request.getObject().getSpec().getManagedSql().getScripts().get(INITIAL_DATA_SCRIPT_INDEX)
          .setSgScript(initDataScript.getMetadata().getName());
      if (request.getObject().getStatus() == null) {
        request.getObject().setStatus(new StackGresClusterStatus());
      }
      if (request.getObject().getStatus().getManagedSql() == null) {
        request.getObject().getStatus().setManagedSql(new StackGresClusterManagedSqlStatus());
      }
      if (request.getObject().getStatus().getManagedSql().getScripts() == null) {
        request.getObject().getStatus().getManagedSql().setScripts(new ArrayList<>());
      }
      request.getObject().getStatus().getManagedSql().getScripts()
          .removeIf(entry -> Objects.equals(INITIAL_DATA_SCRIPT_ID, entry.getId()));
      request.getObject().getStatus().getManagedSql().getScripts()
          .add(INITIAL_DATA_SCRIPT_INDEX, new StackGresClusterManagedScriptEntryStatus());
      request.getObject().getStatus().getManagedSql().getScripts().get(INITIAL_DATA_SCRIPT_INDEX)
          .setId(INITIAL_DATA_SCRIPT_ID);
      request.getObject().getStatus().getManagedSql().getScripts().get(INITIAL_DATA_SCRIPT_INDEX)
          .setStartedAt(Instant.now().toString());
      request.getObject().getStatus().getManagedSql().getScripts().get(INITIAL_DATA_SCRIPT_INDEX)
          .setCompletedAt(Instant.now().toString());
      request.getObject().getStatus().getManagedSql().getScripts().get(INITIAL_DATA_SCRIPT_INDEX)
          .setScripts(new ArrayList<>());
      Seq.seq(request.getObject().getSpec().getInitData().getScripts())
          .zipWithIndex()
          .forEach(initDataScriptEntry -> {
            var managedScriptEntryStatus = new StackGresClusterManagedScriptEntryScriptStatus();
            managedScriptEntryStatus.setId(initDataScriptEntry.v2.intValue());
            managedScriptEntryStatus.setVersion(0);
            request.getObject().getStatus().getManagedSql().getScripts()
                .get(INITIAL_DATA_SCRIPT_INDEX).getScripts()
                .add(managedScriptEntryStatus);
          });
      return true;
    }
    return false;
  }

  private void resetIdForScriptsWithDefaultId(AdmissionRequest<StackGresCluster> request) {
    if (request.getOperation() == Operation.CREATE) {
      request.getObject().getSpec().getManagedSql().getScripts()
          .stream()
          .skip(DEFAULT_SCRIPT_INDEX + 1)
          .filter(script -> script.getId() != null)
          .filter(script -> script.getId().intValue() <= DEFAULT_SCRIPT_INDEX)
          .forEach(script -> script.setId(null));
    }
  }

  private StackGresScript createInitDataScript(AdmissionRequest<StackGresCluster> request) {
    StackGresScript script = new StackGresScript();
    script.setMetadata(new ObjectMeta());
    script.getMetadata().setNamespace(request.getObject().getMetadata().getNamespace());
    script.getMetadata().setName(ManagedSqlUtil.initialDataName(request.getObject()));
    script.setSpec(new StackGresScriptSpec());
    script.getSpec().setScripts(new ArrayList<>());
    Seq.seq(request.getObject().getSpec().getInitData().getScripts())
        .zipWithIndex()
        .forEach(initDataScriptEntry -> {
          StackGresScriptEntry scriptEntry = new StackGresScriptEntry();
          scriptEntry.setId(initDataScriptEntry.v2.intValue());
          scriptEntry.setVersion(0);
          scriptEntry.setName(initDataScriptEntry.v1.getName());
          scriptEntry.setDatabase(initDataScriptEntry.v1.getDatabase());
          if (initDataScriptEntry.v1.getScript() != null) {
            scriptEntry.setScript(initDataScriptEntry.v1.getScript());
          } else {
            scriptEntry.setScriptFrom(new StackGresScriptFrom());
            if (initDataScriptEntry.v1.getScriptFrom().getConfigMapKeyRef() != null) {
              scriptEntry.getScriptFrom().setConfigMapKeyRef(
                  initDataScriptEntry.v1.getScriptFrom().getConfigMapKeyRef());
            } else {
              scriptEntry.getScriptFrom().setSecretKeyRef(
                  initDataScriptEntry.v1.getScriptFrom().getSecretKeyRef());
            }
          }
          script.getSpec().getScripts().add(scriptEntry);
        });
    return script;
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
