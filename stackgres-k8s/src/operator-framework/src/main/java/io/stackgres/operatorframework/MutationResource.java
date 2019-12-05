/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MutationResource<T extends AdmissionReview<?>> {

  Logger logger = LoggerFactory
      .getLogger(MutationResource.class);

  AdmissionReviewResponse mutate(T admissionReview);

  default AdmissionReviewResponse mutate(T admissionReview, JsonPatchMutationPipeline<T> pipeline) {

    AdmissionRequest<?> request = admissionReview.getRequest();
    UUID requestUid = request.getUid();
    logger.info("Mutating admission review " + requestUid.toString()
        + " of kind " + request.getKind().toString());

    AdmissionResponse response = new AdmissionResponse();
    response.setUid(requestUid);

    AdmissionReviewResponse reviewResponse = new AdmissionReviewResponse();
    reviewResponse.setResponse(response);

    reviewResponse.setGroup(admissionReview.getGroup());
    reviewResponse.setKind(admissionReview.getKind());
    reviewResponse.setVersion(admissionReview.getVersion());

    try {
      pipeline.mutate(admissionReview).ifPresent(path -> {
        response.setPatchType("JSONPatch");
        String base64Path = Base64.getEncoder()
            .encodeToString(path.getBytes(StandardCharsets.UTF_8));
        response.setPatch(base64Path);
      });
      response.setAllowed(true);
    } catch (Exception ex) {
      Result result = new Result(500, Optional.ofNullable(ex.getMessage()).orElse("null"));
      logger.error("cannot proceed with request "
          + requestUid.toString() + " cause: " + result.getMessage());
      response.setAllowed(false);
      response.setStatus(result);
    }

    return reviewResponse;

  }
}
