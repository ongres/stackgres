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

import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ScriptsConfigMutator implements ClusterMutator {

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    addDefaultScripts(resource);
    fillRequiredFields(resource);
    updateScriptsStatuses(resource);
    return resource;
  }

  private void addDefaultScripts(StackGresCluster resource) {
    if (resource.getSpec().getManagedSql() == null) {
      resource.getSpec().setManagedSql(new StackGresClusterManagedSql());
    }
    if (resource.getSpec().getManagedSql().getScripts() == null) {
      resource.getSpec().getManagedSql().setScripts(new ArrayList<>());
    }
    String defaultScriptName = ManagedSqlUtil.defaultName(resource);
    if (resource.getSpec().getManagedSql().getScripts().stream()
        .map(StackGresClusterManagedScriptEntry::getSgScript)
        .anyMatch(defaultScriptName::equals)) {
      return;
    }
    resource.getSpec().getManagedSql().getScripts()
        .add(0, new StackGresClusterManagedScriptEntry());
    resource.getSpec().getManagedSql().getScripts().get(0).setId(
        0);
    resource.getSpec().getManagedSql().getScripts().get(0).setSgScript(
        defaultScriptName);
    if (resource.getStatus() == null) {
      resource.setStatus(new StackGresClusterStatus());
    }
    if (resource.getStatus().getManagedSql() == null) {
      resource.getStatus().setManagedSql(new StackGresClusterManagedSqlStatus());
    }
    if (resource.getStatus().getManagedSql().getScripts() == null) {
      resource.getStatus().getManagedSql().setScripts(new ArrayList<>());
    }
    resource.getStatus().getManagedSql().getScripts()
        .removeIf(entry -> Objects.equals(0, entry.getId()));
    resource.getStatus().getManagedSql().getScripts()
        .add(0, new StackGresClusterManagedScriptEntryStatus());
    resource.getStatus().getManagedSql().getScripts().get(0).setId(0);
  }

  private void fillRequiredFields(StackGresCluster resource) {
    int lastId = Optional.of(resource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .stream()
        .flatMap(List::stream)
        .map(StackGresClusterManagedScriptEntry::getId)
        .reduce(-1, (last, id) -> id == null || last >= id ? last : id, (u, v) -> v);
    for (StackGresClusterManagedScriptEntry scriptEntry : Optional.of(resource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getManagedSql)
        .map(StackGresClusterManagedSql::getScripts)
        .orElse(List.of())) {
      if (scriptEntry.getId() == null) {
        lastId++;
        scriptEntry.setId(lastId);
      }
    }
  }

  private void updateScriptsStatuses(StackGresCluster resource) {
    if (resource.getStatus() == null) {
      resource.setStatus(new StackGresClusterStatus());
    }
    if (resource.getStatus().getManagedSql() == null) {
      resource.getStatus().setManagedSql(new StackGresClusterManagedSqlStatus());
    }
    if (resource.getStatus().getManagedSql().getScripts() == null) {
      resource.getStatus().getManagedSql().setScripts(new ArrayList<>());
    }
    List<StackGresClusterManagedScriptEntryStatus> scriptsStatuses =
        resource.getStatus().getManagedSql().getScripts();
    var scriptsToAdd = Optional.of(resource)
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
        .filter(scriptStatus -> Optional.of(resource)
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
  }

  private void addScriptEntryStatus(StackGresClusterManagedScriptEntry scriptEntry,
      List<StackGresClusterManagedScriptEntryStatus> scriptsStatuses) {
    StackGresClusterManagedScriptEntryStatus scriptEntryStatus =
        new StackGresClusterManagedScriptEntryStatus();
    scriptEntryStatus.setId(scriptEntry.getId());
    scriptsStatuses.add(scriptEntryStatus);
  }

}
