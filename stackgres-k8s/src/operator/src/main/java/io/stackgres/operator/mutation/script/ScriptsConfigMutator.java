/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ScriptsConfigMutator
    implements ScriptMutator {

  @Override
  public StackGresScript mutate(StackGresScriptReview review, StackGresScript resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    fillRequiredFields(resource);
    updateScriptsStatuses(resource);
    return resource;
  }

  private void fillRequiredFields(StackGresScript resource) {
    if (Optional.of(resource)
        .map(StackGresScript::getSpec)
        .map(StackGresScriptSpec::isManagedVersions)
        .isEmpty()) {
      resource.getSpec().setManagedVersions(true);
    }
    fillScriptsRequiredFields(resource);
  }

  private void fillScriptsRequiredFields(StackGresScript resource) {
    int lastId = Optional.of(resource)
        .map(StackGresScript::getSpec)
        .map(StackGresScriptSpec::getScripts)
        .stream()
        .flatMap(List::stream)
        .map(StackGresScriptEntry::getId)
        .reduce(-1, (last, id) -> id == null || last >= id ? last : id, (u, v) -> v);
    for (StackGresScriptEntry scriptEntry : Optional.of(resource)
        .map(StackGresScript::getSpec)
        .map(StackGresScriptSpec::getScripts)
        .orElse(List.of())) {
      if (scriptEntry.getId() == null) {
        lastId++;
        scriptEntry.setId(lastId);
      }
      if (scriptEntry.getVersion() == null) {
        scriptEntry.setVersion(0);
      }
    }
  }

  private void updateScriptsStatuses(StackGresScript resource) {
    if (resource.getStatus() == null) {
      resource.setStatus(new StackGresScriptStatus());
    }
    if (resource.getStatus().getScripts() == null) {
      resource.getStatus().setScripts(new ArrayList<>());
    }
    List<StackGresScriptEntryStatus> scriptsStatuses =
        resource.getStatus().getScripts();
    var scriptsToAdd = Optional.of(resource)
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
        .filter(scriptEntryStatus -> Optional.of(resource)
            .map(StackGresScript::getSpec)
            .map(StackGresScriptSpec::getScripts)
            .stream()
            .flatMap(List::stream)
            .noneMatch(scriptEntry -> Objects.equals(
                scriptEntryStatus.v1.getId(), scriptEntry.getId())))
        .reverse()
        .toList();
    scriptsToRemove.forEach(tuple -> scriptsStatuses.remove(tuple.v2.intValue()));
  }

  private void addScriptEntryStatus(StackGresScriptEntry scriptEntry,
      List<StackGresScriptEntryStatus> scriptsStatuses) {
    StackGresScriptEntryStatus scriptEntryStatus =
        new StackGresScriptEntryStatus();
    scriptEntryStatus.setId(scriptEntry.getId());
    scriptsStatuses.add(scriptEntryStatus);
  }

}
