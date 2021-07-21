/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractExtensionsValidator<T extends AdmissionReview<?>>
    implements Validator<T> {

  @Override
  public void validate(T review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE:
      case UPDATE: {
        List<String> requiredExtensions = Seq.seq(getPostgresExtensions(review))
            .flatMap(List::stream)
            .map(StackGresClusterExtension::getName)
            .append(Seq.seq(StackGresUtil.getDefaultClusterExtensions())
                .map(Tuple2::v1))
            .collect(ImmutableList.toImmutableList());
        List<String> toInstallExtensions = Seq.seq(getToInstallExtensions(review))
            .flatMap(List::stream)
            .map(StackGresClusterInstalledExtension::getName)
            .collect(ImmutableList.toImmutableList());
        if (requiredExtensions.stream().anyMatch(requiredExtension -> toInstallExtensions.stream()
            .noneMatch(requiredExtension::equals))) {
          final long missingExtensionsCount = Seq.seq(requiredExtensions)
              .filter(requiredExtension -> toInstallExtensions.stream()
                  .noneMatch(requiredExtension::equals)).count();
          final String missingExtensions = Seq.seq(requiredExtensions)
              .filter(requiredExtension -> toInstallExtensions.stream()
                  .noneMatch(requiredExtension::equals))
              .sorted()
              .toString(", ");
          String errorTypeUri = ErrorType.getErrorTypeUri(ErrorType.MISSING_EXTENSION);
          if (missingExtensionsCount == 1) {
            failValidation(errorTypeUri,
                "Extension " + missingExtensions + " is missing.");
          } else {
            failValidation(errorTypeUri,
                "Extensions " + missingExtensions + " are missing.");
          }
        }
        break;
      }
      default:
    }
  }

  protected abstract Optional<List<StackGresClusterExtension>> getPostgresExtensions(
      T review);

  protected abstract Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      T review);

  protected abstract void failValidation(String reason, String message) throws ValidationFailed;

}
