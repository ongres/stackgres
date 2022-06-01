/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class ScriptsConfigMutator
    implements ClusterMutator {

  static final JsonPointer MANAGED_SQL_POINTER =
      JsonPointer.of("spec", "managedSql");
  static final JsonPointer CONTINUE_ON_SGSCRIPT_ERROR_POINTER =
      MANAGED_SQL_POINTER.append("continueOnSGScriptError");
  static final JsonPointer MANAGED_SQL_SCRIPTS_POINTER = MANAGED_SQL_POINTER.append("scripts");
  static final JsonPointer STATUS_POINTER = JsonPointer.of("status");
  static final JsonPointer MANAGED_SQL_STATUS_POINTER = STATUS_POINTER.append("managedSql");

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    AdmissionRequest<StackGresCluster> request = review.getRequest();
    ImmutableList.Builder<JsonPatchOperation> builder = ImmutableList.builder();
    fillRequiredFields(request, builder);
    return builder.build();
  }

  private void fillRequiredFields(AdmissionRequest<StackGresCluster> request,
      ImmutableList.Builder<JsonPatchOperation> builder) {
    if (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE) {
      int lastId = Optional.of(request.getObject())
          .map(StackGresCluster::getStatus)
          .map(StackGresClusterStatus::getManagedSql)
          .map(StackGresClusterManagedSqlStatus::getLastId)
          .orElse(-1);
      int index = 0;
      for (StackGresClusterManagedScriptEntry scriptEntry : Optional.of(request.getObject())
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getManagedSql)
          .map(StackGresClusterManagedSql::getScripts)
          .orElse(List.of())) {
        if (scriptEntry.getId() == null) {
          lastId++;
          scriptEntry.setId(lastId);
          builder.add(new AddOperation(MANAGED_SQL_SCRIPTS_POINTER.append(index).append("id"),
              FACTORY.numberNode(lastId)));
        }
        index++;
      }
      setStatus(request, builder, lastId);
    }
  }

  private void setStatus(AdmissionRequest<StackGresCluster> request,
      ImmutableList.Builder<JsonPatchOperation> builder, int lastId) {
    if (request.getOperation() == Operation.CREATE
        || request.getOperation() == Operation.UPDATE) {
      ObjectNode managedSqlStatusNode = FACTORY.objectNode();
      managedSqlStatusNode.put("lastId", lastId);
      ArrayNode statusScriptsNode = createStatusScriptsNode(request);
      if (!statusScriptsNode.isEmpty()) {
        managedSqlStatusNode.set("scripts", statusScriptsNode);
      }
      if (request.getObject().getStatus() != null) {
        if (lastId > -1
            || request.getObject().getStatus().getManagedSql() == null
            || !Objects.equals(request.getObject().getStatus().getManagedSql().getLastId(), lastId)
            || isAnyStatusScriptToBeAdded(request)) {
          if (request.getObject().getStatus().getManagedSql() != null) {
            builder.add(new ReplaceOperation(MANAGED_SQL_STATUS_POINTER, managedSqlStatusNode));
          } else {
            builder.add(new AddOperation(MANAGED_SQL_STATUS_POINTER, managedSqlStatusNode));
          }
        }
      } else {
        ObjectNode statusNode = FACTORY.objectNode();
        statusNode.set("managedSql", managedSqlStatusNode);
        builder.add(new AddOperation(STATUS_POINTER, statusNode));
      }
    }
  }

  private boolean isAnyStatusScriptToBeAdded(AdmissionRequest<StackGresCluster> request) {
    var scripts = Optional.of(request.getObject())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .orElse(List.of());
    var statusScripts = Optional.of(request.getObject())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getManagedSql)
        .map(StackGresClusterManagedSqlStatus::getScripts)
        .orElse(List.of());
    return statusScripts.size() != scripts.size()
        || statusScripts
            .stream().anyMatch(
                statusScript -> scripts
                    .stream()
                    .noneMatch(script -> Objects.equals(
                        statusScript.getId(), script.getId())));
  }

  private ArrayNode createStatusScriptsNode(AdmissionRequest<StackGresCluster> request) {
    ArrayNode statusScriptsNode = FACTORY.arrayNode();
    Optional.of(request.getObject())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .forEach(scriptEntry -> addStatusScriptNodeEntry(
            request, scriptEntry, statusScriptsNode));
    return statusScriptsNode;
  }

  private void addStatusScriptNodeEntry(AdmissionRequest<StackGresCluster> request,
      StackGresClusterManagedScriptEntry scriptEntry, ArrayNode statusScriptsNode) {
    ObjectNode statusScriptNode = FACTORY.objectNode();
    statusScriptNode.put("id", scriptEntry.getId());
    Optional.of(request.getObject())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getManagedSql)
        .map(StackGresClusterManagedSqlStatus::getScripts)
        .stream()
        .flatMap(List::stream)
        .filter(statusScriptEntry -> Objects.equals(
            statusScriptEntry.getId(), scriptEntry.getId()))
        .forEach(statusScript -> setStatusScript(statusScriptNode, statusScript));
    statusScriptsNode.add(statusScriptNode);
  }

  private void setStatusScript(ObjectNode statusScriptNode,
      StackGresClusterManagedScriptEntryStatus statusScript) {
    if (statusScript.getStartedAt() != null) {
      statusScriptNode.put("startedAt", statusScript.getStartedAt());
    }
    if (statusScript.getFailedAt() != null) {
      statusScriptNode.put("failedAt", statusScript.getFailedAt());
    }
    if (statusScript.getCompletedAt() != null) {
      statusScriptNode.put("completedAt", statusScript.getCompletedAt());
    }
    
    if (statusScript.getScripts() != null) {
      ArrayNode statusScriptScriptsNode = FACTORY.arrayNode();
      statusScript.getScripts().forEach(statusScriptScript -> setStatusScriptScript(
          statusScriptScriptsNode, statusScriptScript));
      statusScriptNode.set("scripts", statusScriptScriptsNode);
    }
  }

  private void setStatusScriptScript(ArrayNode statusScriptScriptsNode,
      StackGresClusterManagedScriptEntryScriptsStatus statusScriptScript) {
    ObjectNode statusScriptScriptNode = FACTORY.objectNode();
    statusScriptScriptNode.put("id", statusScriptScript.getId());
    statusScriptScriptNode.put("version", statusScriptScript.getVersion());
    if (statusScriptScript.getFailureCode() != null) {
      statusScriptScriptNode.put("failureCode", statusScriptScript.getFailureCode());
    }
    if (statusScriptScript.getFailure() != null) {
      statusScriptScriptNode.put("failure", statusScriptScript.getFailure());
    }
    statusScriptScriptsNode.add(statusScriptScriptNode);
  }

}
