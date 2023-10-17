/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;

public abstract class AbstractReferenceValidator<
      S extends CustomResource<?, ?>,
      T extends AdmissionReview<S>,
      R extends CustomResource<?, ?>>
    implements Validator<T> {

  private final CustomResourceFinder<R> referenceFinder;

  protected AbstractReferenceValidator(CustomResourceFinder<R> referenceFinder) {
    this.referenceFinder = referenceFinder;
  }

  public AbstractReferenceValidator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.referenceFinder = null;
  }

  protected abstract Class<R> getReferenceClass();

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(T review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE: {
        String reference = getReference(review.getRequest().getObject());
        checkIfReferenceExists(review, reference,
            getCreateNotFoundErrorMessage(reference));
        break;
      }
      case UPDATE: {
        String reference = getReference(review.getRequest().getObject());
        checkIfReferenceExists(review, reference,
            getUpdateNotFoundErrorMessage(reference));
        break;
      }
      default:
    }
  }

  protected String getCreateNotFoundErrorMessage(String reference) {
    return HasMetadata.getKind(getReferenceClass())
        + " " + reference + " not found";
  }

  protected String getUpdateNotFoundErrorMessage(String reference) {
    return "Cannot update to "
        + HasMetadata.getKind(getReferenceClass()) + " "
        + reference + " because it doesn't exists";
  }

  protected abstract String getReference(S resource);

  protected String getReferenceNamespace(S resource) {
    return resource.getMetadata().getNamespace();
  }

  private void checkIfReferenceExists(T review, String reference, String onErrorMessage)
      throws ValidationFailed {
    String oldReference = Optional
        .ofNullable(review.getRequest().getOldObject())
        .map(this::getReference)
        .orElse(null);
    if (reference != null
        && checkReferenceFilter(review)
        && (oldReference == null
        || !oldReference.equals(reference))) {
      String referenceNamespace = getReferenceNamespace(review.getRequest().getObject());
      Optional<R> foundReference = referenceFinder.findByNameAndNamespace(
          reference,
          referenceNamespace);

      if (foundReference.isEmpty()) {
        onNotFoundReference(onErrorMessage);
      }
    }
  }

  protected boolean checkReferenceFilter(T review) {
    return true;
  }

  protected abstract void onNotFoundReference(String message) throws ValidationFailed;

}
