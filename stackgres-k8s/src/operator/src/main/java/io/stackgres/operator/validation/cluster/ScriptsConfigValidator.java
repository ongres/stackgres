/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelector;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptFrom;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class ScriptsConfigValidator implements ClusterValidator {

  private final String forbiddenCrUpdateUri = ErrorType
      .getErrorTypeUri(ErrorType.FORBIDDEN_CR_UPDATE);

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
  public void validate(StackGresClusterReview review) throws ValidationFailed {
    StackGresCluster cluster = review.getRequest().getObject();

    List<StackGresClusterScriptEntry> scripts = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getInitData)
        .map(StackGresClusterInitData::getScripts)
        .orElse(ImmutableList.of());

    checkScriptsConfig(review, scripts);
  }

  private void checkScriptsConfig(StackGresClusterReview review,
                                  List<StackGresClusterScriptEntry> scripts)
      throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      List<StackGresClusterScriptEntry> oldScripts = Optional
          .ofNullable(review.getRequest().getOldObject())
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getInitData)
          .map(StackGresClusterInitData::getScripts)
          .orElse(ImmutableList.of());

      if (!Objects.equals(scripts, oldScripts)) {
        fail(forbiddenCrUpdateUri, "Cannot update cluster's scripts configuration");
      }
    } else if (review.getRequest().getOperation() == Operation.CREATE) {
      checkSecretKeySelectors(review, scripts);
      checkConfigMapsSelectors(review, scripts);
    }
  }

  private void checkConfigMapsSelectors(StackGresClusterReview review,
                                        List<StackGresClusterScriptEntry> scripts)
      throws ValidationFailed {
    String clusterNamespace = review.getRequest().getObject().getMetadata().getNamespace();

    List<ConfigMapKeySelector> configMapsSelectors = scripts.stream()
        .map(StackGresClusterScriptEntry::getScriptFrom)
        .filter(Objects::nonNull)
        .map(StackGresClusterScriptFrom::getConfigMapKeyRef)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

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

  private void checkSecretKeySelectors(StackGresClusterReview review,
                                       List<StackGresClusterScriptEntry> scripts)
      throws ValidationFailed {
    String clusterNamespace = review.getRequest().getObject().getMetadata().getNamespace();

    List<SecretKeySelector> secretKeySelectors = scripts.stream()
        .map(StackGresClusterScriptEntry::getScriptFrom)
        .filter(Objects::nonNull)
        .map(StackGresClusterScriptFrom::getSecretKeyRef)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

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
