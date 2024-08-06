/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgstream.StackGresStream;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@ResourceClassForDto(StackGresStream.class)
public final class StreamDto extends ResourceDto {

  private StreamSpec spec;

  private StreamStatus status;

  public StreamSpec getSpec() {
    return spec;
  }

  public void setSpec(StreamSpec spec) {
    this.spec = spec;
  }

  public StreamStatus getStatus() {
    return status;
  }

  public void setStatus(StreamStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
