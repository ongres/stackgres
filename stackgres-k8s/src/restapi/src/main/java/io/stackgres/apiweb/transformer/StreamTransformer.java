/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.stream.StreamDto;
import io.stackgres.apiweb.dto.stream.StreamSpec;
import io.stackgres.apiweb.dto.stream.StreamStatus;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamSpec;
import io.stackgres.common.crd.sgstream.StackGresStreamStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamTransformer extends AbstractResourceTransformer<StreamDto, StackGresStream> {

  private final ObjectMapper mapper;

  @Inject
  public StreamTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresStream toCustomResource(StreamDto source, StackGresStream original) {
    StackGresStream transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresStream.class))
        .orElseGet(StackGresStream::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public StreamDto toDto(StackGresStream source) {
    StreamDto transformation = new StreamDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    return transformation;
  }

  private StackGresStreamSpec getCustomResourceSpec(StreamSpec source) {
    return mapper.convertValue(source, StackGresStreamSpec.class);
  }

  private StreamSpec getResourceSpec(StackGresStreamSpec source) {
    return mapper.convertValue(source, StreamSpec.class);
  }

  private StreamStatus getResourceStatus(StackGresStreamStatus source) {
    return mapper.convertValue(source, StreamStatus.class);
  }

}
