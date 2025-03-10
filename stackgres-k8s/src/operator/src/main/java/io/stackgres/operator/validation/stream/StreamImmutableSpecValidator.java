/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.stream;

import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_CR_UPDATE)
public class StreamImmutableSpecValidator implements StreamValidator {

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresStreamReview review) throws ValidationFailed {

    switch (review.getRequest().getOperation()) {
      case UPDATE:
        StackGresStream stream = review.getRequest().getObject();
        StackGresStream oldStream = review.getRequest().getOldObject();
        if (Optional.ofNullable(stream.getSpec().getMaxRetries()).orElse(-1) >= 0) {
          if (!stream.getSpec().equals(oldStream.getSpec())) {
            fail("spec can not be updated when maxRetries is greater than -1");
          }
        }
        break;
      default:
    }

  }

}
