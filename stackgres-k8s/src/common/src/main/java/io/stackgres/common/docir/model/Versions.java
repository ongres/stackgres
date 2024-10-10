/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record Versions(Version[] versions) {
  public Versions() {
    this(new Version[] {});
  }

  public Versions(List<Version> versions) {
    this(versions.toArray(Version[]::new));
  }
}
