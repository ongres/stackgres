/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionResponse;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MutationResource<T extends AdmissionReview<?>> {

  default Logger getLogger() {
    return LoggerFactory.getLogger(getClass());
  }

  AdmissionReviewResponse mutate(T admissionReview);

  default AdmissionReviewResponse mutate(T admissionReview, JsonPatchMutationPipeline<T> pipeline) {
    AdmissionRequest<?> request = admissionReview.getRequest();
    UUID requestUid = request.getUid();

    getLogger().info("Mutating admission review uid {} of kind {}", requestUid,
        request.getKind().getKind());

    AdmissionResponse response = new AdmissionResponse();
    response.setUid(requestUid);

    AdmissionReviewResponse reviewResponse = new AdmissionReviewResponse();
    reviewResponse.setResponse(response);
    reviewResponse.setKind(admissionReview.getKind());
    reviewResponse.setApiVersion(admissionReview.getApiVersion());

    try {
      pipeline.mutate(admissionReview).ifPresent(path -> {
        response.setPatchType("JSONPatch");
        String base64Path = Base64.getEncoder()
            .encodeToString(path.getBytes(StandardCharsets.UTF_8));
        response.setPatch(base64Path);
      });
      response.setAllowed(true);
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
