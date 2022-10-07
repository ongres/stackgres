/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;

@SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
    justification = "False positive")
public interface DefaultAnnotationMutator
    <R extends CustomResource<?, ?>, T extends AdmissionReview<R>>
    extends JsonPatchMutator<T> {

  JsonPointer ANNOTATION_POINTER = JsonPointer.of("metadata", "annotations");

  @Override
  default List<JsonPatchOperation> mutate(T review) {
    AdmissionRequest<R> request = review.getRequest();
    if (request.getOperation() == Operation.CREATE) {
      return getAnnotationsPatches(request.getObject());
    } else if (request.getOperation() == Operation.UPDATE
        && getAnnotationsToOverwrite(request.getObject()).isPresent()) {
      return getAnnotationsPatches(request.getObject());
    } else {
      return List.of();
    }
  }

  private List<JsonPatchOperation> getAnnotationsPatches(R resource) {
    Optional<Map<String, String>> crAnnotations = Optional
        .ofNullable(resource.getMetadata().getAnnotations());

    Map<String, String> givenAnnotations = crAnnotations.orElseGet(Map::of);

    List<String> existentAnnotations = givenAnnotations.keySet()
        .stream()
        .filter(k -> k.startsWith(StackGresContext.STACKGRES_KEY_PREFIX))
        .toList();

    Map<String, String> defaultAnnotations = getDefaultAnnotationValues();

    Map<String, String> annotationsToAdd = defaultAnnotations.entrySet().stream()
        .filter(e -> !existentAnnotations.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();

    if (crAnnotations.isEmpty()) {
      operations.add(new AddOperation(ANNOTATION_POINTER, FACTORY.objectNode()));
    }

    operations.addAll(buildAnnotationsToAdd(annotationsToAdd));

    getAnnotationsToOverwrite(resource).ifPresent(
        annotationsToOverwrite -> operations.addAll(
            buildAnnotationsToOverwrite(annotationsToOverwrite)));

    return operations.build();
  }

  private Map<String, String> getDefaultAnnotationValues() {
    String operatorVersion = StackGresProperty.OPERATOR_VERSION.getString();

    String operatorVersionKey = StackGresContext.VERSION_KEY;

    return ImmutableMap.<String, String>builder()
        .put(operatorVersionKey, operatorVersion)
        .build();
  }

  private List<JsonPatchOperation> buildAnnotationsToAdd(Map<String, String> annotations) {
    return annotations.entrySet().stream()
        .<JsonPatchOperation>map(entry -> new AddOperation(
            ANNOTATION_POINTER.append(entry.getKey()),
            FACTORY.textNode(entry.getValue())
        ))
        .toList();
  }

  private List<JsonPatchOperation> buildAnnotationsToOverwrite(Map<String, String> annotations) {
    return annotations.entrySet().stream()
        .<JsonPatchOperation>map(entry -> new AddOperation(
            ANNOTATION_POINTER.append(entry.getKey()),
            FACTORY.textNode(entry.getValue())
        ))
        .toList();
  }

  default Optional<Map<String, String>> getAnnotationsToOverwrite(R resource) {
    return Optional.empty();
  }
}
