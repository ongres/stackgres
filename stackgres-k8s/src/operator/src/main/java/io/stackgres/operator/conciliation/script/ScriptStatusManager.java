/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import static io.stackgres.common.ManagedSqlUtil.generateScriptEntryHash;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ScriptStatusManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScriptStatusManager.class);

  private final ResourceFinder<ConfigMap> configMapFinder;

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public ScriptStatusManager(ResourceFinder<ConfigMap> configMapFinder,
      ResourceFinder<Secret> secretFinder) {
    this.configMapFinder = configMapFinder;
    this.secretFinder = secretFinder;
  }

  private static String getScriptId(StackGresScript backup) {
    return backup.getMetadata().getNamespace() + "/" + backup.getMetadata().getName();
  }

  public StackGresScript refreshCondition(StackGresScript source) {
    for (StackGresScriptEntry scriptEntry : Optional.of(source)
        .map(StackGresScript::getSpec)
        .map(StackGresScriptSpec::getScripts)
        .orElse(List.of())) {
      final Optional<String> generatedHash = generateHash(source, scriptEntry);
      if (generatedHash.isEmpty()) {
        continue;
      }
      final String hash = generatedHash.get();
      var statusScriptEntry = Optional.of(source)
          .map(StackGresScript::getStatus)
          .map(StackGresScriptStatus::getScripts)
          .stream()
          .flatMap(List::stream)
          .filter(statusEntry -> Objects.equals(
              statusEntry.getId(), scriptEntry.getId()))
          .findFirst()
          .orElseThrow();
      if (Optional.of(source.getSpec())
          .map(StackGresScriptSpec::isManagedVersions).orElse(true)) {
        setScriptEntryVersion(scriptEntry, hash, statusScriptEntry);
      }
      statusScriptEntry.setHash(hash);
    }
    return source;
  }

  private void setScriptEntryVersion(StackGresScriptEntry scriptEntry, final String hash,
      StackGresScriptEntryStatus statusScriptEntry) {
    final Integer version = scriptEntry.getVersion();
    if (statusScriptEntry.getHash() == null) {
      scriptEntry.setVersion(Optional.ofNullable(version).orElse(0));
    } else if (!Objects.equals(hash, statusScriptEntry.getHash())) {
      scriptEntry.setVersion(Optional.ofNullable(version).map(v -> v + 1).orElse(0));
    }
  }

  private Optional<String> generateHash(StackGresScript source, StackGresScriptEntry scriptEntry) {
    if (scriptEntry.getScript() != null) {
      return Optional.of(generateScriptEntryHash(scriptEntry, scriptEntry.getScript()));
    } else if (Optional.of(scriptEntry).map(StackGresScriptEntry::getScriptFrom)
        .map(StackGresScriptFrom::getConfigMapKeyRef).isPresent()) {
      return configMapFinder.findByNameAndNamespace(
          scriptEntry.getScriptFrom().getConfigMapKeyRef().getName(),
          source.getMetadata().getNamespace())
          .map(ConfigMap::getData)
          .map(data -> data.get(scriptEntry.getScriptFrom().getConfigMapKeyRef().getKey()))
          .map(script -> generateScriptEntryHash(scriptEntry, script));
    } else if (Optional.of(scriptEntry).map(StackGresScriptEntry::getScriptFrom)
        .map(StackGresScriptFrom::getSecretKeyRef).isPresent()) {
      return secretFinder.findByNameAndNamespace(
          scriptEntry.getScriptFrom().getSecretKeyRef().getName(),
          source.getMetadata().getNamespace())
          .map(Secret::getData)
          .map(data -> data.get(scriptEntry.getScriptFrom().getSecretKeyRef().getKey()))
          .map(ResourceUtil::decodeSecret)
          .map(script -> generateScriptEntryHash(scriptEntry, script));
    } else {
      LOGGER.warn("Misconfigured script entry {}", getScriptId(source));
      return Optional.empty();
    }
  }

}
