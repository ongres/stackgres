/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ConversionResource {

  Logger LOGGER = LoggerFactory.getLogger(ConversionResource.class);

  ConversionReviewResponse convert(ConversionReview conversionReview);

  default ConversionReviewResponse convert(
      ConversionPipeline pipeline,
      ConversionReview conversionReview) {
    final ConversionRequest request = conversionReview.getRequest();

    final UUID uid = request.getUid();

    LOGGER.info("Converting conversion review {}", uid.toString());

    ConversionResponse response = new ConversionResponse();
    response.setUid(uid);

    try {

      List<ObjectNode> convertedObjects = pipeline
          .convert(request.getDesiredApiVersion(), request.getObjects());
      response.setConvertedObjects(convertedObjects);
      response.setResult(new StatusBuilder().withStatus("Success").withCode(200).build());

    } catch (Exception ex) {

      String errorMessage = Optional.ofNullable(ex.getMessage()).orElse("null");

      LOGGER.error("Cannot proceed with request {}, cause: {}", uid, errorMessage);

      Status result = new StatusBuilder()
          .withStatus("Failed")
          .withMessage(errorMessage)
          .withCode(500)
          .build();
      response.setResult(result);
    }

    return new ConversionReviewResponse(response);
  }
}
