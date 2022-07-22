/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ScriptsConfigMutator
    implements ScriptMutator {

  static final JsonPointer MANAGED_VERSIONS_POINTER = JsonPointer.of("spec", "managedVersions");

  @Override
  public List<JsonPatchOperation> mutate(StackGresScriptReview review) {
    AdmissionRequest<StackGresScript> request = review.getRequest();
    ImmutableList.Builder<JsonPatchOperation> builder = ImmutableList.builder();
    final var spec = request.getObject().getSpec();
    final var status = request.getObject().getStatus();
    final boolean fillRequiredFields = fillRequiredFields(request);
    if (fillRequiredFields) {
      if (spec == null) {
        builder.add(new AddOperation(SPEC_POINTER,
            FACTORY.pojoNode(request.getObject().getSpec())));
      } else {
        builder.add(new ReplaceOperation(SPEC_POINTER,
            FACTORY.pojoNode(request.getObject().getSpec())));
      }
    }
    final boolean updateScriptsStatuses = updateScriptsStatuses(request);
    if (updateScriptsStatuses) {
      if (status != null) {
        builder.add(new ReplaceOperation(STATUS_POINTER,
            FACTORY.pojoNode(request.getObject().getStatus())));
      } else {
        builder.add(new AddOperation(STATUS_POINTER,
            FACTORY.pojoNode(request.getObject().getStatus())));
      }
    }
    return builder.build();
  }

  private boolean fillRequiredFields(AdmissionRequest<StackGresScript> request) {
    boolean result = false;
    if (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE) {
      if (Optional.of(request.getObject())
          .map(StackGresScript::getSpec)
          .map(StackGresScriptSpec::isManagedVersions)
          .isEmpty()) {
        request.getObject().getSpec().setManagedVersions(true);
        result = true;
      }
      if (fillScriptsRequiredFields(request)) {
        result = true;
      }
    }
    return result;
  }

  private boolean fillScriptsRequiredFields(AdmissionRequest<StackGresScript> request) {
    boolean result = false;
    int lastId = Optional.of(request.getObject())
        .map(StackGresScript::getSpec)
        .map(StackGresScriptSpec::getScripts)
        .stream()
        .flatMap(List::stream)
        .map(StackGresScriptEntry::getId)
        .reduce(-1, (last, id) -> id == null || last >= id ? last : id, (u, v) -> v);
    for (StackGresScriptEntry scriptEntry : Optional.of(request.getObject())
        .map(StackGresScript::getSpec)
        .map(StackGresScriptSpec::getScripts)
        .orElse(List.of())) {
      if (scriptEntry.getId() == null) {
        lastId++;
        scriptEntry.setId(lastId);
        result = true;
      }
      if (scriptEntry.getVersion() == null) {
        scriptEntry.setVersion(0);
        result = true;
      }
    }
    return result;
  }

  private boolean updateScriptsStatuses(AdmissionRequest<StackGresScript> request) {
    if (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE) {
      return createStatusScripts(request);
    }
    return false;
  }

  private boolean createStatusScripts(AdmissionRequest<StackGresScript> request) {
    if (request.getObject().getStatus() == null) {
      request.getObject().setStatus(new StackGresScriptStatus());
    }
    if (request.getObject().getStatus().getScripts() == null) {
      request.getObject().getStatus().setScripts(new ArrayList<>());
    }
    List<StackGresScriptEntryStatus> scriptsStatuses =
        request.getObject().getStatus().getScripts();
    var scriptsToAdd = Optional.of(request.getObject())
        .map(StackGresScript::getSpec)
        .map(StackGresScriptSpec::getScripts)
        .stream()
        .flatMap(List::stream)
        .filter(scriptEntry -> scriptsStatuses
            .stream()
            .noneMatch(scriptEntryStatus -> Objects.equals(
                scriptEntryStatus.getId(), scriptEntry.getId())))
        .toList();
    scriptsToAdd.forEach(scriptEntry -> addScriptEntryStatus(
        scriptEntry, scriptsStatuses));
    var scriptsToRemove = Seq.seq(scriptsStatuses)
        .zipWithIndex()
        .filter(scriptEntryStatus -> Optional.of(request.getObject())
            .map(StackGresScript::getSpec)
            .map(StackGresScriptSpec::getScripts)
            .stream()
            .flatMap(List::stream)
            .noneMatch(scriptEntry -> Objects.equals(
                scriptEntryStatus.v1.getId(), scriptEntry.getId())))
        .reverse()
        .toList();
    scriptsToRemove.forEach(tuple -> scriptsStatuses.remove(tuple.v2.intValue()));
    return !scriptsToAdd.isEmpty() || !scriptsToRemove.isEmpty();
  }

  private void addScriptEntryStatus(StackGresScriptEntry scriptEntry,
      List<StackGresScriptEntryStatus> scriptsStatuses) {
    StackGresScriptEntryStatus scriptEntryStatus =
        new StackGresScriptEntryStatus();
    scriptEntryStatus.setId(scriptEntry.getId());
    scriptsStatuses.add(scriptEntryStatus);
  }

}
