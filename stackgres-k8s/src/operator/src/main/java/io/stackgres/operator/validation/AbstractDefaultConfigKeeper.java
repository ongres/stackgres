/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public abstract class AbstractDefaultConfigKeeper
    <R extends CustomResource, T extends AdmissionReview<R>>
    implements Validator<T> {

  private volatile String installedNamespace;
  private volatile String defaultResourceName;

  private DefaultCustomResourceFactory<R> factory;

  @PostConstruct
  public void init() {
    R defaultResource = factory.buildResource();
    ObjectMeta metadata = defaultResource.getMetadata();
    this.installedNamespace = metadata.getNamespace();
    this.defaultResourceName = metadata.getName();
  }

  @Override
  public void validate(T review) throws ValidationFailed {

    switch (review.getRequest().getOperation()) {
      case UPDATE:
        String updateNamespace = review.getRequest().getObject().getMetadata().getNamespace();
        String updateName = review.getRequest().getObject().getMetadata().getName();
        if (installedNamespace.equals(updateNamespace) && defaultResourceName.equals(updateName)) {
          throw new ValidationFailed("Cannot update default CR" + updateName);
        }
        break;
      case DELETE:
        String deleteNamespace = review.getRequest().getNamespace();
        String deleteName = review.getRequest().getName();
        if (installedNamespace.equals(deleteNamespace) && defaultResourceName.equals(deleteName)) {
          throw new ValidationFailed("Cannot delete default CR " + deleteName);
        }
        break;
      default:
    }
  }

  @Inject
  public void setFactory(DefaultCustomResourceFactory<R> factory) {
    this.factory = factory;
  }
}
