/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.script;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelector;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptEntryStatus;
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ScriptsConfigValidator implements ScriptValidator {

  private final String constraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  private final String invalidReference = ErrorType
      .getErrorTypeUri(ErrorType.INVALID_CR_REFERENCE);

  private final ResourceFinder<Secret> secretFinder;

  private final ResourceFinder<ConfigMap> configMapFinder;

  @Inject
  public ScriptsConfigValidator(ResourceFinder<Secret> secretFinder,
                                ResourceFinder<ConfigMap> configMapFinder) {
    this.secretFinder = secretFinder;
    this.configMapFinder = configMapFinder;
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
      checkSecretKeySelectors(review, scripts);
      checkConfigMapsSelectors(review, scripts);
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

  private void checkConfigMapsSelectors(StackGresScriptReview review,
                                        List<StackGresScriptEntry> scripts)
      throws ValidationFailed {
    String clusterNamespace = review.getRequest().getObject().getMetadata().getNamespace();

    List<ConfigMapKeySelector> configMapsSelectors = scripts.stream()
        .map(StackGresScriptEntry::getScriptFrom)
        .filter(Objects::nonNull)
        .<ConfigMapKeySelector>map(StackGresScriptFrom::getConfigMapKeyRef)
        .filter(Objects::nonNull)
        .toList();

    for (ConfigMapKeySelector configMapRef : configMapsSelectors) {
      Optional<ConfigMap> scriptConfigMap = configMapFinder
          .findByNameAndNamespace(configMapRef.getName(), clusterNamespace);

      if (!scriptConfigMap.isPresent()) {
        fail(invalidReference,
            "Referenced ConfigMap " + configMapRef.getName()
                + " does not exists in namespace " + clusterNamespace);
      } else {
        Set<String> configMapKeys = ImmutableSet.<String>builder()
            .addAll(Optional.ofNullable(scriptConfigMap.get().getData())
                .map(Map::keySet)
                .orElse(Set.of()))
            .addAll(Optional.ofNullable(scriptConfigMap.get().getBinaryData())
                .map(Map::keySet)
                .orElse(Set.of()))
            .build();

        if (!configMapKeys.contains(configMapRef.getKey())) {
          fail(invalidReference, "Key " + configMapRef.getKey()
              + " does not exists in ConfigMap " + configMapRef.getName());
        }
      }

    }

  }

  private void checkSecretKeySelectors(StackGresScriptReview review,
                                       List<StackGresScriptEntry> scripts)
      throws ValidationFailed {
    String clusterNamespace = review.getRequest().getObject().getMetadata().getNamespace();

    List<SecretKeySelector> secretKeySelectors = scripts.stream()
        .map(StackGresScriptEntry::getScriptFrom)
        .filter(Objects::nonNull)
        .<SecretKeySelector>map(StackGresScriptFrom::getSecretKeyRef)
        .filter(Objects::nonNull)
        .toList();

    for (SecretKeySelector secretRef : secretKeySelectors) {
      Optional<Secret> scriptSecret = secretFinder
          .findByNameAndNamespace(secretRef.getName(), clusterNamespace);

      if (!scriptSecret.isPresent()) {
        fail(invalidReference,
            "Referenced Secret " + secretRef.getName()
                + " does not exists in namespace " + clusterNamespace);
      } else if (!scriptSecret.get().getData().containsKey(secretRef.getKey())) {
        fail(invalidReference,
            "Key " + secretRef.getKey()
                + " does not exists in Secret " + secretRef.getName());
      }
    }
  }

}
