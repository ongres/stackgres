/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.admissionwebhook.mutating;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionResponse;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMutationResource<
      R extends HasMetadata, T extends AdmissionReview<R>> {

  private final List<String> allowedNamespaces;
  private final ObjectMapper objectMapper;
  private final MutationPipeline<R, T> pipeline;

  protected AbstractMutationResource(
      List<String> allowedNamespaces,
      ObjectMapper objectMapper,
      MutationPipeline<R, T> pipeline) {
    this.allowedNamespaces = allowedNamespaces;
    this.objectMapper = objectMapper;
    this.pipeline = pipeline;
  }

  protected Logger getLogger() {
    return LoggerFactory.getLogger(getClass());
  }

  public AdmissionReviewResponse mutate(T admissionReview) {
    return mutate(admissionReview, pipeline);
  }

  private AdmissionReviewResponse mutate(
      T admissionReview, MutationPipeline<R, T> pipeline) {
    AdmissionRequest<?> request = admissionReview.getRequest();
    UUID requestUid = request.getUid();

    AdmissionResponse response = new AdmissionResponse();
    response.setUid(requestUid);

    AdmissionReviewResponse reviewResponse = new AdmissionReviewResponse();
    reviewResponse.setResponse(response);
    reviewResponse.setKind(admissionReview.getKind());
    reviewResponse.setApiVersion(admissionReview.getApiVersion());

    try {
      if (allowedNamespaces.isEmpty()
          || allowedNamespaces.contains(admissionReview.getRequest().getNamespace())) {
        getLogger().debug("Mutating admission review uid {} of kind {} for resource {}.{}",
            requestUid, request.getKind().getKind(), request.getNamespace(), request.getName());
        R resourceCopy = copyResource(admissionReview.getRequest().getObject());
        R resourceResult = pipeline.mutate(admissionReview, resourceCopy);
        final JsonNode resourceJson;
        final JsonNode resourceResultJson;
        resourceJson = objectMapper.valueToTree(admissionReview.getRequest().getObject());
        resourceResultJson = objectMapper.valueToTree(resourceResult);
        JsonNode patch = JsonDiff.asJson(resourceJson, resourceResultJson);
        if (!patch.isEmpty()) {
          response.setPatchType("JSONPatch");
          String base64Path = Base64.getEncoder()
              .encodeToString(patch.toString().getBytes(StandardCharsets.UTF_8));
          response.setPatch(base64Path);
        }
      }
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

  private R copyResource(R resource) {
    try {
      return objectMapper.treeToValue(objectMapper.valueToTree(resource), getResourceClass());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  protected abstract Class<R> getResourceClass();

}
