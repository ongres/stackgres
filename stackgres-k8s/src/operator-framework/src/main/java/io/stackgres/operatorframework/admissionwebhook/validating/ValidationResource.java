/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.validating;

import java.util.UUID;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionResponse;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ValidationResource<T extends AdmissionReview<?>> {

  private final ValidationPipeline<T> pipeline;

  protected ValidationResource(ValidationPipeline<T> pipeline) {
    this.pipeline = pipeline;
  }

  Logger getLogger() {
    return LoggerFactory.getLogger(getClass());
  }

  public AdmissionReviewResponse validate(T admissionReview) {
    return validate(admissionReview, pipeline);
  }

  /**
   * Validate a review using a {@code ValidationPipeline}.
   */
  protected AdmissionReviewResponse validate(T admissionReview, ValidationPipeline<T> pipeline) {
    AdmissionRequest<?> request = admissionReview.getRequest();
    UUID requestUid = request.getUid();

    getLogger().info("Validating admission review uid {} of kind {} for resource {}.{}",
        requestUid, request.getKind().getKind(), request.getNamespace(), request.getName());

    AdmissionResponse response = new AdmissionResponse();
    response.setUid(requestUid);

    AdmissionReviewResponse reviewResponse = new AdmissionReviewResponse();
    reviewResponse.setResponse(response);
    reviewResponse.setKind(admissionReview.getKind());
    reviewResponse.setApiVersion(admissionReview.getApiVersion());

    try {
      pipeline.validate(admissionReview);
      response.setAllowed(true);
    } catch (ValidationFailed validationFailed) {
      Status result = validationFailed.getResult();
      response.setAllowed(false);
      response.setStatus(result);

      getLogger().error("cannot proceed with request {} cause: {}",
          requestUid, validationFailed.getMessage(), validationFailed);
    } catch (Exception ex) {
      Status status = new StatusBuilder()
          .withMessage(ex.getMessage() != null ? ex.getMessage() : "Unknown reason")
          .withCode(500)
          .build();
      response.setAllowed(false);
      response.setStatus(status);

      getLogger().error("cannot proceed with request {} cause: {}",
          requestUid, status.getMessage(), ex);
    }
    return reviewResponse;
  }

}
