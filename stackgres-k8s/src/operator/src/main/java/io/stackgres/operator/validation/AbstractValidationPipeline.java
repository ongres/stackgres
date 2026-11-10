/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.CdiUtil;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import jakarta.enterprise.inject.Instance;

public abstract class AbstractValidationPipeline<T extends AdmissionReview<?>>
    extends io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline<T> {

  public AbstractValidationPipeline(Instance<Validator<T>> validatorInstances) {
    super(validatorInstances.stream()
        .sorted(validationTypeComparator())
        .toList());
  }

  public AbstractValidationPipeline() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  static Comparator<Validator<?>> validationTypeComparator() {
    return (v1, v2) -> {
      final ValidationType v1ValidationType = v1.getClass().getAnnotation(ValidationType.class);
      final ValidationType v2ValidationType = v2.getClass().getAnnotation(ValidationType.class);

      if (v1ValidationType == null && v2ValidationType == null) {
        return 0;
      } else if (v1ValidationType == null) {
        return -1;
      } else if (v2ValidationType == null) {
        return 1;
      } else {
        return v1ValidationType.value().compareTo(v2ValidationType.value());
      }
    };
  }

  @Override
  public void validate(T review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.UPDATE) {
      final HasMetadata object = review.getRequest().getObject();
      final HasMetadata oldObject = review.getRequest().getOldObject();

      if (Objects.equals(object, oldObject)) {
        return;
      }

      // The deletion of a custom resource that has a finalizer only completes when the operator
      // removes it, an update that the API server sends to this pipeline like any other. A
      // validation that reads another resource, as the SGShardedCluster an SGShardedDbOps operates
      // on, fails once that resource has been deleted, and the rejected update leaves the
      // finalizer where it is: the custom resource, and the namespace that holds it, then stay
      // terminating forever. A custom resource that is being deleted is not validated, since its
      // spec will not be acted upon anymore.
      if (Optional.ofNullable(object)
          .map(HasMetadata::getMetadata)
          .map(ObjectMeta::getDeletionTimestamp)
          .isPresent()) {
        return;
      }
    }

    super.validate(review);
  }

}
