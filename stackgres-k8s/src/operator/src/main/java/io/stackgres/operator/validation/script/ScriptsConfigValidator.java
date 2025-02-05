/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.script;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ScriptsConfigValidator implements ScriptValidator {

  private final String constraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  @Inject
  public ScriptsConfigValidator() {
  }

  @Override
  public void validate(StackGresScriptReview review) throws ValidationFailed {
    StackGresScript script = review.getRequest().getObject();
    if (review.getRequest().getOperation() == Operation.UPDATE
        || review.getRequest().getOperation() == Operation.CREATE) {
      List<StackGresScriptEntry> scripts = Optional.of(script.getSpec())
          .map(StackGresScriptSpec::getScripts)
          .orElse(List.of());
      List<StackGresScriptEntryStatus> scriptsStatuses = Optional.of(script)
          .map(StackGresScript::getStatus)
          .map(StackGresScriptStatus::getScripts)
          .orElse(List.of());
      checkIdsUniqueness(scripts);
      checkStatusIdsCorrelation(scripts, scriptsStatuses);
    }
  }

  private void checkIdsUniqueness(List<StackGresScriptEntry> scripts) throws ValidationFailed {
    if (scripts.stream()
        .collect(Collectors.groupingBy(StackGresScriptEntry::getId))
        .values()
        .stream()
        .anyMatch(list -> list.size() > 1)) {
      fail(constraintViolationUri, "Script entries must contain unique ids");
    }
  }

  private void checkStatusIdsCorrelation(List<StackGresScriptEntry> scripts,
      List<StackGresScriptEntryStatus> scriptsStatuses) throws ValidationFailed {
    if (scripts.stream()
        .map(StackGresScriptEntry::getId)
        .anyMatch(scriptId -> scriptsStatuses.stream()
            .map(StackGresScriptEntryStatus::getId)
            .noneMatch(scriptId::equals))) {
      fail(constraintViolationUri, "Script entries must contain a matching id"
          + " for each script status entry");
    }
    if (scriptsStatuses.stream()
        .map(StackGresScriptEntryStatus::getId)
        .anyMatch(scriptStatusId -> scripts.stream()
            .map(StackGresScriptEntry::getId)
            .noneMatch(scriptStatusId::equals))) {
      fail(constraintViolationUri, "Script status entries must contain a matching id"
          + " for each script entry");
    }
  }

}
