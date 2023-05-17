/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ScriptsConfigValidator implements ClusterValidator {

  private final String constraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  @Override
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();
    if (review.getRequest().getOperation() == Operation.UPDATE
        || review.getRequest().getOperation() == Operation.CREATE) {
      List<StackGresClusterManagedScriptEntry> scripts = Optional.of(cluster.getSpec())
          .map(StackGresClusterSpec::getManagedSql)
          .map(StackGresClusterManagedSql::getScripts)
          .orElse(List.of());
      List<StackGresClusterManagedScriptEntryStatus> scriptsStatuses =
          Optional.ofNullable(cluster.getStatus())
          .map(StackGresClusterStatus::getManagedSql)
          .map(StackGresClusterManagedSqlStatus::getScripts)
          .orElse(List.of());
      checkIdsUniqueness(scripts);
      checkStatusIdsCorrelation(scripts, scriptsStatuses);
    }
  }

  private void checkIdsUniqueness(
      List<StackGresClusterManagedScriptEntry> scripts) throws ValidationFailed {
    if (scripts.stream()
        .collect(Collectors.groupingBy(StackGresClusterManagedScriptEntry::getId))
        .values()
        .stream()
        .anyMatch(list -> list.size() > 1)) {
      fail(constraintViolationUri, "Script entries must contain unique ids");
    }
  }

  private void checkStatusIdsCorrelation(List<StackGresClusterManagedScriptEntry> scripts,
      List<StackGresClusterManagedScriptEntryStatus> scriptsStatuses) throws ValidationFailed {
    if (scripts.stream()
        .map(StackGresClusterManagedScriptEntry::getId)
        .anyMatch(scriptId -> scriptsStatuses.stream()
            .map(StackGresClusterManagedScriptEntryStatus::getId)
            .noneMatch(scriptId::equals))) {
      fail(constraintViolationUri, "Script entries must contain a matching id"
          + " for each script status entry");
    }
    if (scriptsStatuses.stream()
        .map(StackGresClusterManagedScriptEntryStatus::getId)
        .anyMatch(scriptStatusId -> scripts.stream()
            .map(StackGresClusterManagedScriptEntry::getId)
            .noneMatch(scriptStatusId::equals))) {
      fail(constraintViolationUri, "Script status entries must contain a matching id"
          + " for each script entry");
    }
  }

}
