/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.validating;

import java.util.UUID;

import io.fabric8.kubernetes.api.model.Status;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionResponse;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ValidationResource<T extends AdmissionReview<?>> {

  Logger logger = LoggerFactory
      .getLogger(ValidationResource.class);

  AdmissionReviewResponse validate(T admissionReview);

  /**
   * Validate a review using a {@code ValidationPipeline}.
   */
  default AdmissionReviewResponse validate(T admissionReview, ValidationPipeline<T> pipeline) {

    AdmissionRequest<?> request = admissionReview.getRequest();
    UUID requestUid = request.getUid();
    logger.info("Validating admission review " + requestUid.toString()
        + " of kind " + request.getKind().toString());

    AdmissionResponse response = new AdmissionResponse();
    response.setUid(requestUid);

    AdmissionReviewResponse reviewResponse = new AdmissionReviewResponse();
    reviewResponse.setResponse(response);

    reviewResponse.setGroup(admissionReview.getGroup());
    reviewResponse.setKind(admissionReview.getKind());
    reviewResponse.setVersion(admissionReview.getVersion());

    try {
      pipeline.validate(admissionReview);
      response.setAllowed(true);
    } catch (ValidationFailed validationFailed) {
      Status result = validationFailed.getResult();
      logger.error("cannot proceed with request "
          + requestUid + " cause: " + result.getMessage());
      response.setAllowed(false);
      response.setStatus(result);
    }

    return reviewResponse;

  }

}
