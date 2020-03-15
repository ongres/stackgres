/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ErrorType;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public abstract class AbstractDefaultConfigKeeper
    <R extends CustomResource, T extends AdmissionReview<R>>
    implements Validator<T> {

  private volatile String installedNamespace;
  private volatile String defaultResourceName;

  private DefaultCustomResourceFactory<R> factory;
  private ConfigContext configContext;

  private String errorTypeUri;

  @PostConstruct
  public void init() {
    R defaultResource = factory.buildResource();
    ObjectMeta metadata = defaultResource.getMetadata();
    this.installedNamespace = metadata.getNamespace();
    this.defaultResourceName = metadata.getName();
    this.errorTypeUri = configContext.getErrorTypeUri(ErrorType.DEFAULT_CONFIGURATION);
  }

  @Override
  public void validate(T review) throws ValidationFailed {

    final AdmissionRequest<R> request = review.getRequest();
    switch (request.getOperation()) {
      case UPDATE:
        String updateNamespace = request.getObject().getMetadata().getNamespace();
        String updateName = request.getObject().getMetadata().getName();
        if (installedNamespace.equals(updateNamespace) && defaultResourceName.equals(updateName)) {
          final String message = "Cannot update CR " + updateName + " because is a default CR";
          fail(request.getKind().getKind(), errorTypeUri, message);
        }
        break;
      case DELETE:
        String deleteNamespace = request.getNamespace();
        String deleteName = request.getName();
        if (installedNamespace.equals(deleteNamespace) && defaultResourceName.equals(deleteName)) {
          final String message = "Cannot delete CR " + deleteName + " because is a default CR";
          fail(request.getKind().getKind(), errorTypeUri, message);
        }
        break;
      default:
    }
  }

  @Inject
  public void setFactory(DefaultCustomResourceFactory<R> factory) {
    this.factory = factory;
  }

  @Inject
  public void setConfigContext(ConfigContext configContext) {
    this.configContext = configContext;
  }
}
