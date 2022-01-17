/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * When converting a resource from client-side-apply to server-side-apply it must be sanitized.
 * See also https://github.com/kubernetes/kubernetes/issues/98024
 * </p>
 * <p>
 * The server-side-apply does things differently from client-side-apply.
 * In particular it must validate list-map-keys.
 * See https://kubernetes.io/docs/reference/using-api/server-side-apply/#merge-strategy
 * </p>
 * <p>
 * This class read "x-kubernetes-list-map-keys" field from Kubernetes Open API for arrays
 * and search for duplicates in the list. If found it removes those in the last positions.
 * </p>
 */
public class ServerSideApplySanitizer {

  private static final JsonPointer SPEC_POINTER = JsonPointer.of("spec");
  private static final JsonPointer STATUS_POINTER = JsonPointer.of("status");
  private static final List<Tuple2<String, JsonPointer>> POINTERS_TO_REMOVE_FROM_SPECIFIC_TYPE =
      ImmutableList.of(
          Tuple.tuple(getTypeOf(Service.class), SPEC_POINTER.append("clusterIP")),
          Tuple.tuple(getTypeOf(ServiceAccount.class), JsonPointer.of("secrets")));

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ServerSideApplySanitizer.class);

  private final ObjectNode openApi;

  public ServerSideApplySanitizer(ObjectNode openApi) {
    this.openApi = openApi;
  }

  @SuppressWarnings("unchecked")
  public <M extends HasMetadata> M sanitize(M deployedResource) {
    JsonNode object = Serialization.jsonMapper().valueToTree(deployedResource);
    try {
      return (M) Serialization.jsonMapper().treeToValue(
          sanitize(object), deployedResource.getClass());
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      throw new RuntimeException(ex);
    }
  }

  public JsonNode sanitize(JsonNode resource) {
    if (!resource.has("apiVersion") || !resource.has("kind")) {
      throw new IllegalArgumentException(
          "The resource must define apiVersion and kind properties");
    }
    JsonNode sanitizedResource = resource.deepCopy();
    String[] apiVersion = resource.get("apiVersion").asText().split("/");
    final String kind;
    if (apiVersion.length == 1) {
      kind = constructType("", apiVersion[0], resource.get("kind").asText());
    } else {
      kind = constructType(apiVersion[0], apiVersion[1], resource.get("kind").asText());
    }
    sanitize(kind, JsonPointer.empty(), sanitizedResource);
    if (LOGGER.isDebugEnabled()) {
      JsonPatch patch = JsonDiff.asJsonPatch(resource, sanitizedResource);
      LOGGER.debug("Sanitizer patch for {}.{} of kind {}: {}",
          Optional.of(resource).map(n -> n.get("metadata")).map(n -> n.get("name"))
          .map(JsonNode::asText).orElse("<unknown>"),
          Optional.of(resource).map(n -> n.get("metadata")).map(n -> n.get("name"))
          .map(JsonNode::asText).orElse("<unknown>"),
          kind, patch);
    }
    return sanitizedResource;
  }

  private void sanitize(String type, JsonPointer pointer, JsonNode node) {
    if (node instanceof ObjectNode) {
      sanitize(type, pointer, (ObjectNode) node);
    } else if (node instanceof ArrayNode) {
      sanitize(type, pointer, (ArrayNode) node);
    }
  }

  private void sanitize(String type, JsonPointer pointer,
      ObjectNode object) {
    Seq.seq(object.fields()).toList().forEach(field -> sanitize(
        type, pointer.append(field.getKey()), field.getValue()));
    remove(pointer, object, STATUS_POINTER);
    POINTERS_TO_REMOVE_FROM_SPECIFIC_TYPE
        .stream()
        .filter(t -> t.v1.equals(type))
        .forEach(t -> remove(pointer, object, t.v2));
  }

  private void sanitize(String type, JsonPointer pointer, ArrayNode array) {
    var elements = Seq.seq(array.elements()).zipWithIndex().toList();
    elements.forEach(element -> sanitize(type,
        pointer.append(element.v2.intValue()), element.v1));
    if (hasOpenApiTypeListMapKeys(type, pointer)) {
      List<String> listMapKeys = getOpenApiTypeListMapKeys(type, pointer);
      Seq.seq(elements)
          .grouped(element -> listMapKeys.stream().map(element.v1::get)
              .collect(ImmutableList.toImmutableList()))
          .flatMap(t -> t.v2.skip(1))
          .reverse()
          .forEach(element -> array.remove(element.v2.intValue()));
    }
  }

  private void remove(JsonPointer pointer, ObjectNode object, JsonPointer pointerToRemove) {
    final String lastField = Seq.seq(pointerToRemove).findLast().orElseThrow()
        .getToken().getRaw();
    if (pointer.equals(pointerToRemove.parent())
        && object.has(lastField)) {
      object.remove(lastField);
    }
  }

  private boolean hasOpenApiTypeListMapKeys(String type,
      JsonPointer pointer) {
    return getOpenApiPath(type, pointer)
        .map(object -> object.has("x-kubernetes-list-map-keys"))
        .orElse(false);
  }

  private List<String> getOpenApiTypeListMapKeys(String type,
      JsonPointer pointer) {
    return getOpenApiPath(type, pointer)
        .map(object -> object.get("x-kubernetes-list-map-keys"))
        .stream()
        .filter(ArrayNode.class::isInstance)
        .map(ArrayNode.class::cast)
        .flatMap(listMapKeys -> Seq.seq(listMapKeys))
        .map(JsonNode::asText)
        .collect(ImmutableList.toImmutableList());
  }

  private Optional<ObjectNode> getOpenApiPath(String type, JsonPointer pointer) {
    ObjectNode objectType = getOpenApiType(type);

    try (var seq = Seq.seq(pointer)) {
      return seq.reduce(
          Tuple.tuple(Optional.<ObjectNode>empty(), JsonPointer.empty(), objectType, false),
          (context, token) -> {
            if (context.v4) {
              return context;
            }
            if (context.v3 == null) {
              return context;
            }
            String rawToken = token.getToken().getRaw();
            if (context.v3.has("$ref")) {
              String ref = context.v3.get("$ref").asText();
              if (!ref.startsWith("#/definitions/")) {
                throw new IllegalArgumentException(
                    "Invalid $ref " + ref + " (type: " + context.v3.getNodeType() + ")"
                        + " found under path " + context.v2 + " for type " + type);
              }
              String refType = ref.substring("#/definitions/".length());
              JsonPointer refPointer = createRelativePointer(pointer, context.v2);
              Optional<ObjectNode> refResult = getOpenApiPath(refType, refPointer);
              return context.map1(v -> refResult)
                  .map4(v -> true);
            }
            if (!context.v3.has("type")) {
              LOGGER.warn("Missing field \"type\" in path {} for definition of {}",
                  context.v2, type);
              return context;
            }
            String innerType = context.v3.get("type").asText();
            JsonPointer nextPointer = context.v2.append(rawToken);
            if ("object".equals(innerType)) {
              var propertyObjectNode = Optional.of(context.v3)
                  .map(node -> {
                    if (!node.has("properties")) {
                      LOGGER.warn("Missing field \"properties\" in path {} for definition of {}",
                          context.v2, type);
                    }
                    return node;
                  })
                  .map(node -> node.get("properties"))
                  .map(node -> {
                    if (!node.has(rawToken)) {
                      LOGGER.warn("Missing field \"{}\" in path {} for definition of {}",
                          rawToken, context.v2, type);
                    }
                    return node;
                  })
                  .map(node -> node.get(rawToken))
                  .filter(ObjectNode.class::isInstance)
                  .map(ObjectNode.class::cast);
              return context
                  .map1(v -> propertyObjectNode.filter(o -> pointer.equals(nextPointer)))
                  .map2(v -> v.append(rawToken))
                  .map3(v -> propertyObjectNode.orElse(null))
                  .map4(v -> pointer.equals(nextPointer));
            } else if ("array".equals(innerType)) {
              var elementObjectNode = Optional.of(context.v3)
                  .map(node -> {
                    if (!node.has("items")) {
                      LOGGER.warn("Missing field \"items\" in path {} for definition of {}",
                          context.v2, type);
                    }
                    return node;
                  })
                  .map(node -> node.get("items"))
                  .filter(ObjectNode.class::isInstance)
                  .map(ObjectNode.class::cast);
              return context
                  .map1(v -> elementObjectNode.filter(o -> pointer.equals(nextPointer)))
                  .map2(v -> v.append(rawToken))
                  .map3(v -> elementObjectNode.orElse(null))
                  .map4(v -> pointer.equals(nextPointer));
            }
            return context;
          }, (u, v) -> v).v1;
    }
  }

  private JsonPointer createRelativePointer(JsonPointer pointer, JsonPointer prefix) {
    var iterator = pointer.iterator();
    prefix.forEach(token -> iterator.next());
    return Seq.seq(iterator).reduce(JsonPointer.empty(),
        (relative, token) -> relative.append(token.getToken().getRaw()),
        (u, v) -> v);
  }

  private ObjectNode getOpenApiType(String type) {
    if (openApi.get("definitions").has(type)) {
      return (ObjectNode) openApi.get("definitions").get(type);
    }
    Optional<ObjectNode> definitionByGroupVersionKind =
        Seq.seq(openApi.get("definitions").elements())
        .filter(ObjectNode.class::isInstance)
        .map(ObjectNode.class::cast)
        .filter(definition -> Optional.ofNullable(definition.get("x-kubernetes-group-version-kind"))
            .filter(ArrayNode.class::isInstance)
            .map(ArrayNode.class::cast)
            .stream()
            .flatMap(Seq::seq)
            .filter(this::hasGroupVersionKind)
            .anyMatch(groupVersionKind -> isSameGroupVersionKind(type, groupVersionKind)))
        .findFirst();
    return definitionByGroupVersionKind
        .orElseThrow(() -> new IllegalArgumentException(
            "Type " + type + " not defined in kubernetes open API"));
  }

  private boolean hasGroupVersionKind(JsonNode groupVersionKind) {
    return groupVersionKind.has("group")
        && groupVersionKind.has("version")
        && groupVersionKind.has("kind");
  }

  private boolean isSameGroupVersionKind(String type,
      JsonNode groupVersionKind) {
    return type.equals(constructType(
        groupVersionKind.get("group").asText(),
        groupVersionKind.get("version").asText(),
        groupVersionKind.get("kind").asText()));
  }

  private static String getTypeOf(Class<? extends HasMetadata> resourceClass) {
    final String group = HasMetadata.getGroup(resourceClass);
    final String apiVersion = HasMetadata.getApiVersion(resourceClass);
    final String kind = HasMetadata.getKind(resourceClass);
    return constructType(
        group,
        apiVersion.substring(apiVersion.indexOf("/") + 1),
        kind);
  }

  private static String constructType(String group, String apiVersion, String kind) {
    return group + "." + apiVersion + "." + kind;
  }
}
