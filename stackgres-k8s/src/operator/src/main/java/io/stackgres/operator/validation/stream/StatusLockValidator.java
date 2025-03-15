/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.stream;

import static io.stackgres.operatorframework.resource.ResourceUtil.getServiceAccountFromUsername;
import static io.stackgres.operatorframework.resource.ResourceUtil.isServiceAccountUsername;

import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stackgres.common.ErrorType;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.common.StackGresStreamReview;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ValidationType(ErrorType.FORBIDDEN_STREAM_UPDATE)
public class StatusLockValidator implements StreamValidator {

  final ObjectMapper objectMapper;
  final int duration;

  @Inject
  public StatusLockValidator(OperatorPropertyContext operatorPropertyContext,
      ObjectMapper objectMapper) {
    this.duration = operatorPropertyContext.getInt(OperatorProperty.LOCK_DURATION);
    this.objectMapper = objectMapper;
  }

  @Override
  @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT",
      justification = "False positive")
  public void validate(StackGresStreamReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case UPDATE: {
        StackGresStream stream = review.getRequest().getObject();
        StackGresStream oldStream = review.getRequest().getOldObject();
        if (Objects.equals(objectMapper.valueToTree(stream.getStatus()),
            objectMapper.valueToTree(oldStream.getStatus()))) {
          return;
        }
        String username = review.getRequest().getUserInfo().getUsername();
        if (StackGresUtil.isLocked(stream)
            && (
                username == null
                || !isServiceAccountUsername(username)
                || !Objects.equals(
                    StackGresUtil.getLockServiceAccount(stream),
                    getServiceAccountFromUsername(username))
                )
            ) {
          fail("SGStream status update is forbidden. It is locked by the SGStream"
              + " that is currently running. Please, wait for the operation to finish,"
              + " stop the operation by deleting it or wait for the lock duration of "
              + duration + " seconds to expire.");
        }
        break;
      }
      default:
    }

  }

}
