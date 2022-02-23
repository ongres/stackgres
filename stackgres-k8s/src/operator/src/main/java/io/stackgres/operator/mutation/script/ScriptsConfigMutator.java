/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@ApplicationScoped
public class ScriptsConfigMutator
    implements ScriptMutator {

  static final JsonPointer MANAGED_VERSIONS_POINTER = JsonPointer.of("spec", "managedVersions");
  static final JsonPointer SCRIPTS_POINTER = JsonPointer.of("spec", "scripts");
  static final JsonPointer STATUS_POINTER = JsonPointer.of("status");

  @Override
  public List<JsonPatchOperation> mutate(StackGresScriptReview review) {
    AdmissionRequest<StackGresScript> request = review.getRequest();
    ImmutableList.Builder<JsonPatchOperation> builder = ImmutableList.builder();
    fillRequiredFields(request, builder);
    return builder.build();
  }

  private void fillRequiredFields(AdmissionRequest<StackGresScript> request,
      ImmutableList.Builder<JsonPatchOperation> builder) {
    if (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE) {
      if (Optional.of(request.getObject())
          .map(StackGresScript::getSpec)
          .map(StackGresScriptSpec::isManagedVersions)
          .isEmpty()) {
        request.getObject().getSpec().setManagedVersions(true);
        builder.add(new AddOperation(MANAGED_VERSIONS_POINTER,
            FACTORY.booleanNode(true)));
      }
      int lastId = Optional.of(request.getObject())
          .map(StackGresScript::getStatus)
          .map(StackGresScriptStatus::getLastId)
          .orElse(-1);
      int index = 0;
      for (StackGresScriptEntry scriptEntry : Optional.of(request.getObject())
          .map(StackGresScript::getSpec)
          .map(StackGresScriptSpec::getScripts)
          .orElse(List.of())) {
        if (scriptEntry.getId() == null) {
          lastId++;
          scriptEntry.setId(lastId);
          builder.add(new AddOperation(SCRIPTS_POINTER.append(index).append("id"),
              FACTORY.numberNode(lastId)));
        }
        if (scriptEntry.getVersion() == null) {
          builder.add(new AddOperation(SCRIPTS_POINTER.append(index).append("version"),
              FACTORY.numberNode(0)));
        }
        index++;
      }
      setStatus(request, builder, lastId);
    }
  }

  private void setStatus(AdmissionRequest<StackGresScript> request,
      ImmutableList.Builder<JsonPatchOperation> builder, int lastId) {
    if (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE) {
      ObjectNode statusNode = FACTORY.objectNode();
      statusNode.put("lastId", lastId);
      ArrayNode statusScriptsNode = createStatusScriptsNode(request);
      if (!statusScriptsNode.isEmpty()) {
        statusNode.set("scripts", statusScriptsNode);
      }
      if (request.getObject().getStatus() != null) {
        if (!Objects.equals(request.getObject().getStatus().getLastId(), lastId)
            || isAnyStatusScriptToBeAdded(request)) {
          builder.add(new ReplaceOperation(STATUS_POINTER, statusNode));
        }
      } else {
        builder.add(new AddOperation(STATUS_POINTER, statusNode));
      }
    }
  }

  private boolean isAnyStatusScriptToBeAdded(AdmissionRequest<StackGresScript> request) {
    var scripts = Optional.of(request.getObject())
        .map(StackGresScript::getSpec)
        .map(StackGresScriptSpec::getScripts)
        .orElse(List.of());
    var statusScripts = Optional.of(request.getObject())
        .map(StackGresScript::getStatus)
        .map(StackGresScriptStatus::getScripts)
        .orElse(List.of());
    return statusScripts.size() != scripts.size()
        || statusScripts
            .stream().anyMatch(
                statusScript -> scripts
                    .stream()
                    .noneMatch(script -> Objects.equals(
                        statusScript.getId(), script.getId())));
  }

  private ArrayNode createStatusScriptsNode(AdmissionRequest<StackGresScript> request) {
    ArrayNode statusScriptsNode = FACTORY.arrayNode();
    Optional.of(request.getObject())
        .map(StackGresScript::getSpec)
        .map(StackGresScriptSpec::getScripts)
        .stream()
        .flatMap(List::stream)
        .forEach(scriptEntry -> addStatusScriptNodeEntry(request, scriptEntry, statusScriptsNode));
    return statusScriptsNode;
  }

  private void addStatusScriptNodeEntry(AdmissionRequest<StackGresScript> request,
      StackGresScriptEntry scriptEntry, ArrayNode statusScriptsNode) {
    ObjectNode statusScriptNode = FACTORY.objectNode();
    statusScriptsNode.add(statusScriptNode);
    statusScriptNode.put("id", scriptEntry.getId());
    Optional.of(request.getObject())
        .map(StackGresScript::getStatus)
        .map(StackGresScriptStatus::getScripts)
        .stream()
        .flatMap(List::stream)
        .filter(statusScriptEntry -> Objects.equals(
            statusScriptEntry.getId(), scriptEntry.getId()))
        .findFirst()
        .map(StackGresScriptEntryStatus::getHash)
        .ifPresent(hash -> statusScriptNode.put("hash", hash));
  }

}
