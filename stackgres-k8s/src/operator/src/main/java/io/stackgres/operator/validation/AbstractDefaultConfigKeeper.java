/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ErrorType;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public abstract class AbstractDefaultConfigKeeper
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>>
    implements Validator<T> {

  private final String errorTypeUri =
      ErrorType.getErrorTypeUri(ErrorType.DEFAULT_CONFIGURATION);

  private Map<String, Set<String>> installedResources;
  private Instance<DefaultCustomResourceFactory<R>> factories;

  @PostConstruct
  public void init() {
    this.installedResources = factories.stream()
        .map(DefaultCustomResourceFactory::buildResource)
        .map(CustomResource::getMetadata)
        .collect(Collectors.groupingBy(ObjectMeta::getNamespace,
            Collectors.mapping(ObjectMeta::getName, Collectors.toSet())));
  }

  @Override
  public void validate(T review) throws ValidationFailed {
    final AdmissionRequest<R> request = review.getRequest();
    switch (request.getOperation()) {
      case UPDATE:
        final R object = request.getObject();

        String updateNamespace = object.getMetadata().getNamespace();
        String updateName = object.getMetadata().getName();
        if (installedResources.containsKey(updateNamespace)
            && installedResources.get(updateNamespace).contains(updateName)) {
          final String message = "Cannot update CR " + updateName + " because is a default CR";
          fail(request.getKind().getKind(), errorTypeUri, message);
        }
        break;
      case DELETE:
        String deleteNamespace = request.getNamespace();
        String deleteName = request.getName();
        if (installedResources.containsKey(deleteNamespace)
            && installedResources.get(deleteNamespace).contains(deleteName)) {
          final String message = "Cannot delete CR " + deleteName + " because is a default CR";
          fail(request.getKind().getKind(), errorTypeUri, message);
        }
        break;
      default:
        break;
    }
  }

  @Inject
  public void setFactories(Instance<DefaultCustomResourceFactory<R>> factories) {
    this.factories = factories;
  }

}
