/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir.model;

import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record BaseImages(BaseImage[] baseImages) {
  public BaseImages(Stream<BaseImage> baseImages) {
    this(baseImages.toArray(BaseImage[]::new));
  }
}
