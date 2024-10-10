/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.common.constraint.NotNull;

/**
 * The {@code /image-url} request body.
 *
 * @param name when set, the name of the repository under which docir serves the resolved image,
 *        that is the {@code <name>} of the {@code <host>/<name>@sha256:<digest>} reference
 *        returned in {@code urlDigest}. StackGres sets a descriptive name (see
 *        {@link io.stackgres.common.docir.DocirImageIndex#getImageName()}) so that the reference
 *        of the image of a container tells which components and versions it contains.
 */
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageUrlRequest(
    String name,
    String tshirtSize,
    boolean flavorAndBaseOmmitted,
    String revision,
    BaseRequest base,
    FlavorRequest flavor,
    AddonRequest[] addons
) {

  @RegisterForReflection
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record BaseRequest(
      String name,
      String majorVersion,
      String minorVersion,
      String revision) {
  }

  @RegisterForReflection
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FlavorRequest(
      String name,
      FlavorVersionRequest[] versions) {
  }

  @RegisterForReflection
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FlavorVersionRequest(
      String majorVersion,
      String minorVersion,
      String revision,
      ExtensionRequest[] extensions) {
  }

  @RegisterForReflection
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record AddonRequest(
      @NotNull
      String name,
      String version,
      String revision) {
  }

  @RegisterForReflection
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ExtensionRequest(
      @NotNull
      String name,
      String version,
      String revision) {
  }

}
