/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir.model;

import java.io.IOException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.docir.model.ImageUrl.Image.PlatformLayer;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageUrl(
        @NotNull Image image
) {
  @RegisterForReflection
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Image(
      @NotNull String url,
      @NotNull String urlDigest,
      @NotNull String imageId,
      Platform[] platforms,
      String revision,
      Base base,
      Flavor flavor,
      Addon[] addons) {
    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Base(
        @NotNull
        String name,
        String majorVersion,
        String minorVersion,
        @JsonSerialize(using = LayersSerializer.class)
        @JsonDeserialize(using = LayersDeserializer.class)
        PlatformLayer[] layers) {
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Flavor(
        @NotNull
        String name,
        FlavorVersion[] versions) {
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlavorVersion(
        String majorVersion,
        String minorVersion,
        @JsonSerialize(using = LayersSerializer.class)
        @JsonDeserialize(using = LayersDeserializer.class)
        PlatformLayer[] layers,
        Extension[] extensions) {
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Extension(
        @NotNull
        String name,
        String version,
        @JsonSerialize(using = LayersSerializer.class)
        @JsonDeserialize(using = LayersDeserializer.class)
        PlatformLayer[] layers) {
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Addon(
        @NotNull
        String name,
        String version,
        @JsonSerialize(using = LayersSerializer.class)
        @JsonDeserialize(using = LayersDeserializer.class)
        PlatformLayer[] layers) {
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlatformLayer(
        @NotNull
        String name,
        String revision,
        Long size) {
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlatformLayerWithoutName(
        String revision,
        Long size) {
      static PlatformLayerWithoutName from(PlatformLayer layer) {
        return new PlatformLayerWithoutName(layer.revision(), layer.size());
      }
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Platform(
        @NotNull
        String name,
        @NotNull
        String os,
        @NotNull
        String architecture,
        String variant) {
    }
  }

  @RegisterForReflection
  public static class LayersSerializer extends JsonSerializer<ImageUrl.Image.PlatformLayer[]> {

    public LayersSerializer() {
      super();
    }

    @Override
    public void serialize(
        ImageUrl.Image.PlatformLayer[] value, 
        JsonGenerator gen,
        SerializerProvider serializers) throws IOException, JsonProcessingException {
      gen.writeStartObject();
      for (var layer : value) {
        gen.writeFieldName(layer.name());
        gen.writeObject(ImageUrl.Image.PlatformLayerWithoutName.from(layer));
      }
      gen.writeEndObject();
    }
  }

  @RegisterForReflection
  public static class LayersDeserializer extends JsonDeserializer<ImageUrl.Image.PlatformLayer[]> {

    public LayersDeserializer() {
      super();
    }

    @Override
    public PlatformLayer[] deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JacksonException {
      JsonNode node = p.readValueAsTree();
      if (node.isObject()) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                node.fields(),
                Spliterator.ORDERED),
            false)
            .map(field -> new PlatformLayer(
                field.getKey(),
                Optional.ofNullable(field.getValue().get("revision"))
                    .map(JsonNode::asText).orElse(null),
                Optional.ofNullable(field.getValue().get("size"))
                    .map(JsonNode::asLong).orElse(null)))
            .toArray(PlatformLayer[]::new);
      }
      return StreamSupport.stream(
          Spliterators.spliteratorUnknownSize(
              node.elements(),
              Spliterator.ORDERED),
          false)
          .map(element -> new PlatformLayer(
              Optional.ofNullable(element.get("name")).map(JsonNode::asText).orElse(null),
              Optional.ofNullable(element.get("revision")).map(JsonNode::asText).orElse(null),
              Optional.ofNullable(element.get("size")).map(JsonNode::asLong).orElse(null)))
          .toArray(PlatformLayer[]::new);
    }

  }

}

